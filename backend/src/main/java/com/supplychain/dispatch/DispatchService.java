package com.supplychain.dispatch;

import com.supplychain.common.PageParams;
import com.supplychain.common.PageResponse;
import com.supplychain.common.SearchTerm;
import com.supplychain.dispatch.DispatchDtos.DispatchResponse;
import com.supplychain.dispatch.DispatchDtos.DispatchStatusRequest;
import com.supplychain.exception.ApiException;
import com.supplychain.inventory.Inventory;
import com.supplychain.inventory.InventoryService;
import com.supplychain.order.Order;
import com.supplychain.order.OrderItem;
import com.supplychain.order.OrderItemRepository;
import com.supplychain.order.OrderPriority;
import com.supplychain.order.OrderRepository;
import com.supplychain.order.OrderStatus;
import com.supplychain.tracking.DeliveryTracking;
import com.supplychain.tracking.DeliveryTrackingRepository;
import com.supplychain.user.User;
import com.supplychain.user.UserRepository;
import com.supplychain.warehouse.Warehouse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DispatchService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");
    private static final double HOURS_PER_DAY = 24.0;
    private static final Map<String, String> SORTABLE = Map.of(
            "createdAt", "createdAt",
            "estimatedDeliveryDate", "estimatedDeliveryDate",
            "dispatchDate", "dispatchDate");

    private final DispatchRepository dispatches;
    private final DeliveryTrackingRepository tracking;
    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final InventoryService inventoryService;
    private final UserRepository users;
    private final Clock clock;

    public DispatchService(
            DispatchRepository dispatches,
            DeliveryTrackingRepository tracking,
            OrderRepository orders,
            OrderItemRepository orderItems,
            InventoryService inventoryService,
            UserRepository users,
            Clock clock) {
        this.dispatches = dispatches;
        this.tracking = tracking;
        this.orders = orders;
        this.orderItems = orderItems;
        this.inventoryService = inventoryService;
        this.users = users;
        this.clock = clock;
    }

    /** Created in the same transaction that reserves the stock, so a confirmed order always has one. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createFor(Order order, Warehouse warehouse, BigDecimal etaHours, Instant at) {
        Dispatch dispatch = dispatches.save(new Dispatch(
                order,
                warehouse,
                "TRK-%d-%06d".formatted(at.atZone(ZoneOffset.UTC).getYear(), order.getId()),
                ShipmentStatus.PENDING,
                estimatedDeliveryDate(order.getPriority(), etaHours, at),
                at));
        record(dispatch, null, ShipmentStatus.PENDING, null, "Dispatch created", at);
    }

    /** Whole days of estimated time, plus a day of margin for Standard orders. */
    static LocalDate estimatedDeliveryDate(OrderPriority priority, BigDecimal etaHours, Instant at) {
        long days = (long) Math.ceil(etaHours.doubleValue() / HOURS_PER_DAY);
        long margin = priority == OrderPriority.STANDARD ? 1 : 0;
        return at.atZone(BUSINESS_ZONE).toLocalDate().plusDays(days + margin);
    }

    /** Moves a shipment one step forward, keeping its order and the stock ledger in step with it. */
    @Transactional
    public DispatchResponse updateStatus(long id, DispatchStatusRequest request, long actorId) {
        long orderId = dispatches.findOrderIdById(id).orElseThrow(DispatchService::notFound);
        // The order's lock comes first, as in cancellation, so that the two can't interleave.
        Order order = orders.findByIdForUpdate(orderId).orElseThrow(DispatchService::notFound);
        Dispatch dispatch = dispatches.findByIdForUpdate(id).orElseThrow(DispatchService::notFound);

        ShipmentStatus from = dispatch.getStatus();
        ShipmentStatus to = request.status();
        if (from.next().filter(to::equals).isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, transitionError(from, to));
        }

        Instant now = clock.instant();
        dispatch.setStatus(to);
        switch (to) {
            case PACKED -> order.setStatus(OrderStatus.PROCESSING);
            case DISPATCHED -> {
                shipStock(order, dispatch, now);
                dispatch.setDispatchDate(now);
                order.setStatus(OrderStatus.DISPATCHED);
            }
            case DELIVERED -> {
                dispatch.setDeliveryDate(now);
                order.setStatus(OrderStatus.DELIVERED);
            }
            default -> {
                // IN_TRANSIT changes only the shipment: the order stays DISPATCHED.
            }
        }
        record(dispatch, from, to, users.getReferenceById(actorId), blankToNull(request.note()), now);
        dispatches.flush();
        return DispatchResponse.from(dispatch);
    }

    /** Called while the order's lock is held, and only for a shipment that hasn't left the warehouse. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelFor(Order order, long actorId, Instant at) {
        dispatches.findByOrderIdForUpdate(order.getId()).ifPresent(dispatch -> {
            ShipmentStatus from = dispatch.getStatus();
            dispatch.setStatus(ShipmentStatus.CANCELLED);
            record(dispatch, from, ShipmentStatus.CANCELLED, users.getReferenceById(actorId), "Order cancelled", at);
        });
    }

    /** The goods physically leave, so the units stop being reserved. Available stock doesn't change. */
    private void shipStock(Order order, Dispatch dispatch, Instant at) {
        List<OrderItem> items = orderItems.findWithProductByOrderId(order.getId());
        List<Long> productIds = items.stream().map(item -> item.getProduct().getId()).sorted().toList();
        List<Inventory> locked = inventoryService.lockForProducts(productIds);
        for (OrderItem item : items) {
            Inventory row = locked.stream()
                    .filter(stock -> stock.getWarehouse().getId().equals(dispatch.getWarehouse().getId())
                            && stock.getProduct().getId().equals(item.getProduct().getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("The reserved stock row is missing"));
            inventoryService.ship(row, order, item.getQuantity(), at);
        }
    }

    private void record(
            Dispatch dispatch, ShipmentStatus from, ShipmentStatus to, User actor, String note, Instant at) {
        tracking.save(new DeliveryTracking(dispatch, from, to, actor, note, at));
    }

    private static String transitionError(ShipmentStatus from, ShipmentStatus to) {
        if (to == ShipmentStatus.CANCELLED) {
            return "A shipment is cancelled by cancelling its order.";
        }
        return from.next()
                .map(next -> "A shipment that is %s can only move to %s, not %s."
                        .formatted(label(from), label(next), label(to)))
                .orElseGet(() -> "This shipment is already %s, so its status can't change.".formatted(label(from)));
    }

    private static String label(ShipmentStatus status) {
        return status.name().replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "This dispatch doesn't exist.");
    }

    @Transactional(readOnly = true)
    public PageResponse<DispatchResponse> list(ShipmentStatus status, Long warehouseId, String q, PageParams params) {
        Sort newestFirst = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        return PageResponse.from(dispatches
                .search(status, warehouseId, SearchTerm.contains(q), params.toPageable(SORTABLE, newestFirst))
                .map(DispatchResponse::from));
    }

    @Transactional(readOnly = true)
    public DispatchResponse get(long id) {
        return DispatchResponse.from(dispatches.findWithPartiesById(id).orElseThrow(DispatchService::notFound));
    }
}
