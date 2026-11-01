package com.supplychain.order;

import com.supplychain.allocation.AllocationService;
import com.supplychain.allocation.location.LocationService;
import com.supplychain.allocation.location.LocationService.City;
import com.supplychain.common.PageParams;
import com.supplychain.common.PageResponse;
import com.supplychain.common.SearchTerm;
import com.supplychain.dispatch.DispatchService;
import com.supplychain.exception.ApiException;
import com.supplychain.inventory.Inventory;
import com.supplychain.inventory.InventoryService;
import com.supplychain.order.OrderDtos.OrderCreateRequest;
import com.supplychain.order.OrderDtos.OrderDetailResponse;
import com.supplychain.order.OrderDtos.OrderItemRequest;
import com.supplychain.order.OrderDtos.OrderItemResponse;
import com.supplychain.order.OrderDtos.OrderSummaryResponse;
import com.supplychain.order.OrderItemRepository.UnitCount;
import com.supplychain.product.Product;
import com.supplychain.product.ProductRepository;
import com.supplychain.product.ProductStatus;
import com.supplychain.security.AuthenticatedUser;
import com.supplychain.user.UserRepository;
import com.supplychain.user.UserRole;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrderService {

    private static final String STATE = "Karnataka";
    private static final Map<String, String> SORTABLE =
            Map.of("createdAt", "createdAt", "totalAmount", "totalAmount", "orderNumber", "orderNumber");
    /** Stock has not left the warehouse in these states, so it can still be released. */
    private static final Set<OrderStatus> CANCELLABLE =
            Set.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PROCESSING);
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final ProductRepository products;
    private final UserRepository users;
    private final LocationService locations;
    private final AllocationService allocationService;
    private final InventoryService inventoryService;
    private final DispatchService dispatchService;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public OrderService(
            OrderRepository orders,
            OrderItemRepository orderItems,
            ProductRepository products,
            UserRepository users,
            LocationService locations,
            AllocationService allocationService,
            InventoryService inventoryService,
            DispatchService dispatchService,
            TransactionTemplate transaction,
            Clock clock) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.products = products;
        this.users = users;
        this.locations = locations;
        this.allocationService = allocationService;
        this.inventoryService = inventoryService;
        this.dispatchService = dispatchService;
        this.transaction = transaction;
        this.clock = clock;
    }

    /**
     * Places an order at most once per idempotency key, so a double-click or a retry after a lost
     * response returns the original order. The order, its decision and its reservation commit together:
     * only a successful reservation confirms an order, and a failure anywhere undoes all of them. If no
     * warehouse can supply the whole order, it stays PENDING and UNPAID, with the decision on record.
     */
    public OrderDetailResponse place(OrderCreateRequest request, String idempotencyKey, long customerId) {
        if (idempotencyKey.length() < 8 || idempotencyKey.length() > 64) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The Idempotency-Key header must be 8 to 64 characters.");
        }
        Optional<OrderDetailResponse> existing = findExisting(customerId, idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }
        try {
            return transaction.execute(status -> toDetail(create(request, idempotencyKey, customerId)));
        } catch (DataIntegrityViolationException | PessimisticLockingFailureException ex) {
            // A simultaneous request with this key committed first. Its order is the answer.
            return findExisting(customerId, idempotencyKey).orElseThrow(() -> ex);
        }
    }

    private Optional<OrderDetailResponse> findExisting(long customerId, String idempotencyKey) {
        return transaction.execute(status ->
                orders.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey).map(this::toDetail));
    }

    private Order create(OrderCreateRequest request, String idempotencyKey, long customerId) {
        City city = locations.find(request.deliveryCity())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "We don't deliver to this city yet."));

        Map<Long, Integer> quantities = mergeLines(request.items());
        Map<Long, Product> lineProducts = products.findAllById(quantities.keySet()).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> line : quantities.entrySet()) {
            Product product = lineProducts.get(line.getKey());
            if (product == null || product.getStatus() != ProductStatus.ACTIVE) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        (product == null ? "A product" : product.getName())
                                + " in your cart is no longer available. Remove it and try again.");
            }
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(line.getValue())));
        }

        Instant now = clock.instant();
        Order order = new Order(
                // The real number includes the database ID, so it is assigned just after this insert.
                "TEMP-" + UUID.randomUUID().toString().substring(0, 24),
                users.getReferenceById(customerId),
                OrderStatus.PENDING,
                request.priority(),
                PaymentStatus.UNPAID,
                total,
                request.deliveryName().trim(),
                request.deliveryPhone().trim(),
                request.deliveryAddress().trim(),
                city.name(),
                STATE,
                request.deliveryPostalCode().trim(),
                city.latitude(),
                city.longitude(),
                idempotencyKey,
                now);
        orders.saveAndFlush(order);
        order.setOrderNumber("ORD-%d-%06d".formatted(now.atZone(ZoneOffset.UTC).getYear(), order.getId()));

        List<OrderItem> items = new ArrayList<>();
        quantities.forEach((productId, quantity) -> {
            Product product = lineProducts.get(productId);
            items.add(orderItems.save(new OrderItem(
                    order,
                    product,
                    quantity,
                    product.getPrice(),
                    product.getPrice().multiply(BigDecimal.valueOf(quantity)))));
        });
        allocationService.allocate(order, items);
        orders.flush();
        return order;
    }

    /** Combines repeated lines for a product, keeping the order they first appeared in. */
    private static Map<Long, Integer> mergeLines(List<OrderItemRequest> items) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderItemRequest item : items) {
            quantities.merge(item.productId(), item.quantity(), Integer::sum);
        }
        return quantities;
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse get(long id, AuthenticatedUser viewer) {
        return toDetail(visibleTo(viewer, orders.findWithPartiesById(id)));
    }

    /** For use inside a transaction: the order with its customer and warehouse, if this viewer may see it. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Order requireVisible(long id, AuthenticatedUser viewer) {
        return visibleTo(viewer, orders.findWithPartiesById(id));
    }

    /** Someone else's order looks the same as a missing one, so order IDs can't be probed. */
    private static Order visibleTo(AuthenticatedUser viewer, Optional<Order> order) {
        return order.filter(found ->
                        viewer.role() == UserRole.ADMIN || found.getCustomer().getId() == viewer.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "This order doesn't exist."));
    }

    /** Runs allocation again for an order that had no eligible warehouse, for example after restocking. */
    @Transactional
    public OrderDetailResponse reallocate(long id) {
        Order order = orders.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "This order doesn't exist."));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Only a pending order can be allocated again.");
        }
        allocationService.allocate(order, orderItems.findWithProductByOrderId(id));
        orders.flush();
        return toDetail(order);
    }

    /** Takes the order's lock first, so simultaneous cancels can't release the same stock twice. */
    @Transactional
    public OrderDetailResponse cancel(long id, AuthenticatedUser viewer) {
        Order order = visibleTo(viewer, orders.findByIdForUpdate(id));
        if (!CANCELLABLE.contains(order.getStatus())) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "This order can't be cancelled, because it is already " + describe(order) + ".");
        }

        Instant now = clock.instant();
        if (order.getSelectedWarehouse() != null) {
            releaseStock(order, now);
            dispatchService.cancelFor(order, viewer.id(), now);
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(now);
        if (order.getPaymentStatus() == PaymentStatus.PAID_SIMULATED) {
            order.setPaymentStatus(PaymentStatus.REFUNDED_SIMULATED);
        }
        orders.flush();
        return toDetail(order);
    }

    private void releaseStock(Order order, Instant at) {
        List<OrderItem> items = orderItems.findWithProductByOrderId(order.getId());
        List<Long> productIds = items.stream().map(item -> item.getProduct().getId()).sorted().toList();
        List<Inventory> locked = inventoryService.lockForProducts(productIds);
        for (OrderItem item : items) {
            Inventory row = locked.stream()
                    .filter(stock -> stock.getWarehouse().getId().equals(order.getSelectedWarehouse().getId())
                            && stock.getProduct().getId().equals(item.getProduct().getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("The reserved stock row is missing"));
            inventoryService.release(row, order, item.getQuantity(), at);
        }
    }

    private static String describe(Order order) {
        return order.getStatus().name().toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> listForCustomer(long customerId, PageParams params) {
        return list(customerId, null, null, null, params);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> list(
            Long customerId, OrderStatus status, Long warehouseId, String q, PageParams params) {
        Page<Order> page = orders.search(
                customerId, status, warehouseId, SearchTerm.contains(q), params.toPageable(SORTABLE, NEWEST_FIRST));
        Map<Long, Long> units = page.isEmpty()
                ? Map.of()
                : orderItems.countUnits(page.map(Order::getId).getContent()).stream()
                        .collect(Collectors.toMap(UnitCount::orderId, UnitCount::units));
        return PageResponse.from(page.map(order -> new OrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomer().getFullName(),
                order.getCreatedAt(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getPriority(),
                order.getTotalAmount(),
                units.getOrDefault(order.getId(), 0L),
                warehouseName(order))));
    }

    private OrderDetailResponse toDetail(Order order) {
        List<OrderItemResponse> items = orderItems.findWithProductByOrderId(order.getId()).stream()
                .map(item -> new OrderItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getProduct().getSku(),
                        item.getProduct().getImageUrl(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()))
                .toList();
        return new OrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomer().getFullName(),
                order.getCreatedAt(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getPriority(),
                order.getTotalAmount(),
                warehouseName(order),
                order.getDeliveryName(),
                order.getDeliveryPhone(),
                order.getDeliveryAddress(),
                order.getDeliveryCity(),
                order.getDeliveryState(),
                order.getDeliveryPostalCode(),
                order.getConfirmedAt(),
                order.getCancelledAt(),
                CANCELLABLE.contains(order.getStatus()),
                items);
    }

    private static String warehouseName(Order order) {
        return order.getSelectedWarehouse() == null ? null : order.getSelectedWarehouse().getName();
    }
}
