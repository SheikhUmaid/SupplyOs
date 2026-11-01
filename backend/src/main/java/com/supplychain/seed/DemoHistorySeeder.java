package com.supplychain.seed;

import com.supplychain.config.AdjustableClock;
import com.supplychain.dispatch.DispatchDtos.DispatchStatusRequest;
import com.supplychain.dispatch.DispatchRepository;
import com.supplychain.dispatch.DispatchService;
import com.supplychain.dispatch.ShipmentStatus;
import com.supplychain.inventory.Inventory;
import com.supplychain.inventory.InventoryDtos.InventoryUpdateRequest;
import com.supplychain.inventory.InventoryDtos.StockChangeType;
import com.supplychain.inventory.InventoryRepository;
import com.supplychain.inventory.InventoryService;
import com.supplychain.order.OrderDtos.OrderCreateRequest;
import com.supplychain.order.OrderDtos.OrderDetailResponse;
import com.supplychain.order.OrderDtos.OrderItemRequest;
import com.supplychain.order.OrderPriority;
import com.supplychain.order.OrderService;
import com.supplychain.order.OrderStatus;
import com.supplychain.product.Product;
import com.supplychain.product.ProductRepository;
import com.supplychain.security.AuthenticatedUser;
import com.supplychain.user.User;
import com.supplychain.user.UserRepository;
import com.supplychain.user.UserRole;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Ten customers and two months of orders, placed through the real services with the clock moved back.
 * Allocations, the stock ledger and tracking history are therefore what the system really produced,
 * and the analytics are true to the data. The list is fixed, so every run gives the same result.
 *
 * <p>Events run in time order, as they would have happened, so each allocation sees the stock of its
 * moment. Recorded adjustments then return stock to its opening values, which is what the required
 * demonstration scenario needs. Customer passwords come from SEED_CUSTOMER_PASSWORD, never from source.
 */
@Component
@Profile("dev")
public class DemoHistorySeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoHistorySeeder.class);

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int STARTS_DAYS_AGO = 75;
    private static final String STOCK_RESET_REASON = "Set to demonstration stock level";

    private record CustomerSeed(String name, String email, String phone, String address, String city, String pin) {}

    private record Line(String sku, int quantity) {}

    private enum Outcome {
        AWAITING_STOCK, CONFIRMED, PACKED, DISPATCHED, IN_TRANSIT, DELIVERED, DELIVERED_LATE, CANCELLED
    }

    /** `customer` is an index into CUSTOMERS. Times are hours of the Indian day. */
    private record OrderSeed(
            int daysAgo, int hour, int customer, OrderPriority priority, Outcome outcome, List<Line> lines) {}

    /** `warehouse` is the warehouse's name. */
    private record RestockSeed(int daysAgo, String sku, String warehouse, int quantity) {}

    private record Event(Instant at, Runnable action) {}

    private static final String HUBLI = "Hubli Warehouse";
    private static final String BENGALURU = "Bengaluru Warehouse";
    private static final String MYSURU = "Mysuru Warehouse";

    private static final List<CustomerSeed> CUSTOMERS = List.of(
            new CustomerSeed("Asha Rao", "asha.rao@demo.local", "9845000101", "14 PJ Extension", "Davanagere", "577002"),
            new CustomerSeed("Rahul Kulkarni", "rahul.kulkarni@demo.local", "9845000102", "22 Tilakwadi", "Belagavi", "590006"),
            new CustomerSeed("Priya Nair", "priya.nair@demo.local", "9845000103", "8 Kadri Road", "Mangaluru", "575002"),
            new CustomerSeed("Vikram Shetty", "vikram.shetty@demo.local", "9845000104", "31 Kunjibettu", "Udupi", "576102"),
            new CustomerSeed("Meera Patil", "meera.patil@demo.local", "9845000105", "5 Vidyanagar", "Hubli", "580021"),
            new CustomerSeed("Arjun Gowda", "arjun.gowda@demo.local", "9845000106", "19 Kuvempunagar", "Mysuru", "570023"),
            new CustomerSeed("Kavya Reddy", "kavya.reddy@demo.local", "9845000107", "47 Indiranagar", "Bengaluru", "560038"),
            new CustomerSeed("Sanjay Hegde", "sanjay.hegde@demo.local", "9845000108", "3 Gandhi Bazaar", "Shivamogga", "577202"),
            new CustomerSeed("Nisha Joshi", "nisha.joshi@demo.local", "9845000109", "11 Vidyanagar", "Hassan", "573201"),
            new CustomerSeed("Imran Khan", "imran.khan@demo.local", "9845000110", "26 SS Puram", "Tumakuru", "572102"));

    private static final OrderPriority STANDARD = OrderPriority.STANDARD;
    private static final OrderPriority EXPRESS = OrderPriority.EXPRESS;

    private static final List<OrderSeed> ORDERS = List.of(
            order(60, 11, 6, STANDARD, Outcome.DELIVERED, line("CO-3001", 1), line("AC-4002", 1)),
            order(58, 15, 0, STANDARD, Outcome.DELIVERED, line("AU-2001", 1)),
            order(55, 10, 4, EXPRESS, Outcome.DELIVERED, line("PH-1001", 1), line("AC-4003", 1)),
            order(52, 17, 5, STANDARD, Outcome.DELIVERED, line("AC-4001", 2)),
            order(49, 12, 1, STANDARD, Outcome.DELIVERED_LATE, line("AC-4003", 2), line("AC-4002", 1)),
            order(46, 9, 7, STANDARD, Outcome.DELIVERED, line("PH-1002", 1)),
            order(44, 18, 2, STANDARD, Outcome.CANCELLED, line("SD-5001", 1)),
            order(41, 13, 9, EXPRESS, Outcome.DELIVERED, line("AU-2001", 2)),
            order(38, 11, 3, STANDARD, Outcome.DELIVERED, line("AU-2002", 1), line("AC-4003", 1)),
            order(35, 16, 6, STANDARD, Outcome.DELIVERED, line("PH-1001", 2)),
            order(33, 10, 8, STANDARD, Outcome.DELIVERED, line("SD-5002", 1), line("AC-4002", 2)),
            order(30, 14, 4, STANDARD, Outcome.DELIVERED, line("AU-2001", 1), line("AC-4001", 1)),
            order(27, 9, 0, EXPRESS, Outcome.DELIVERED, line("SD-5001", 1)),
            order(25, 17, 5, STANDARD, Outcome.DELIVERED_LATE, line("CO-3001", 1)),
            order(22, 12, 2, STANDARD, Outcome.DELIVERED, line("AC-4003", 3)),
            order(20, 15, 7, STANDARD, Outcome.CANCELLED, line("PH-1002", 1)),
            order(17, 11, 1, STANDARD, Outcome.DELIVERED, line("AU-2001", 2), line("AC-4002", 1)),
            order(14, 10, 9, STANDARD, Outcome.DELIVERED, line("SD-5003", 1)),
            order(12, 16, 3, EXPRESS, Outcome.DELIVERED, line("PH-1001", 1)),
            order(9, 13, 6, STANDARD, Outcome.DELIVERED, line("AC-4002", 1), line("AC-4001", 1)),
            order(7, 9, 8, STANDARD, Outcome.DELIVERED, line("AU-2002", 2)),
            order(5, 14, 5, STANDARD, Outcome.DELIVERED, line("AC-4003", 1), line("AC-4002", 2)),
            order(4, 11, 4, STANDARD, Outcome.IN_TRANSIT, line("AU-2001", 1)),
            order(3, 12, 1, STANDARD, Outcome.IN_TRANSIT, line("SD-5002", 1)),
            order(3, 10, 2, STANDARD, Outcome.DISPATCHED, line("AC-4001", 1), line("AC-4003", 1)),
            order(2, 11, 7, EXPRESS, Outcome.DISPATCHED, line("SD-5001", 1)),
            order(2, 9, 9, STANDARD, Outcome.PACKED, line("PH-1002", 1)),
            order(1, 12, 3, STANDARD, Outcome.PACKED, line("AC-4002", 1)),
            order(1, 10, 8, STANDARD, Outcome.CONFIRMED, line("AU-2002", 1)),
            order(1, 11, 0, STANDARD, Outcome.CONFIRMED, line("AC-4003", 1)),
            // No warehouse holds 6 monitors, though 12 exist in total, and orders are never split.
            order(1, 9, 2, STANDARD, Outcome.AWAITING_STOCK, line("CO-3002", 6)));

    private static final List<RestockSeed> RESTOCKS = List.of(
            new RestockSeed(50, "AC-4003", HUBLI, 20),
            new RestockSeed(43, "AU-2001", BENGALURU, 15),
            new RestockSeed(36, "PH-1001", BENGALURU, 12),
            new RestockSeed(28, "AC-4002", BENGALURU, 18),
            new RestockSeed(21, "AC-4001", MYSURU, 10),
            new RestockSeed(13, "AU-2002", HUBLI, 8),
            new RestockSeed(6, "AC-4003", MYSURU, 14));

    private static OrderSeed order(
            int daysAgo, int hour, int customer, OrderPriority priority, Outcome outcome, Line... lines) {
        return new OrderSeed(daysAgo, hour, customer, priority, outcome, List.of(lines));
    }

    private static Line line(String sku, int quantity) {
        return new Line(sku, quantity);
    }

    private final UserRepository users;
    private final ProductRepository products;
    private final InventoryRepository inventory;
    private final DispatchRepository dispatches;
    private final OrderService orderService;
    private final DispatchService dispatchService;
    private final InventoryService inventoryService;
    private final PasswordEncoder passwordEncoder;
    private final AdjustableClock clock;
    private final String customerPassword;

    public DemoHistorySeeder(
            UserRepository users,
            ProductRepository products,
            InventoryRepository inventory,
            DispatchRepository dispatches,
            OrderService orderService,
            DispatchService dispatchService,
            InventoryService inventoryService,
            PasswordEncoder passwordEncoder,
            AdjustableClock clock,
            @Value("${SEED_CUSTOMER_PASSWORD:}") String customerPassword) {
        this.users = users;
        this.products = products;
        this.inventory = inventory;
        this.dispatches = dispatches;
        this.orderService = orderService;
        this.dispatchService = dispatchService;
        this.inventoryService = inventoryService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.customerPassword = customerPassword;
    }

    /** History needs a password for its customers and an administrator to perform the shipment steps. */
    public boolean isConfigured() {
        if (!StringUtils.hasText(customerPassword)) {
            log.warn("SEED_CUSTOMER_PASSWORD is not set, so no demonstration history was created.");
            return false;
        }
        if (users.findFirstByRoleOrderByIdAsc(UserRole.ADMIN).isEmpty()) {
            log.warn("There is no administrator, so no demonstration history was created.");
            return false;
        }
        return true;
    }

    public Instant start() {
        return at(STARTS_DAYS_AGO, 9);
    }

    /** Runs inside the caller's transaction, with the clock already moved to `start()`. */
    public void seed() {
        long adminId = users.findFirstByRoleOrderByIdAsc(UserRole.ADMIN).orElseThrow().getId();
        List<Long> customerIds = createCustomers();
        Map<String, Long> productIds = products.findAll().stream()
                .collect(Collectors.toMap(Product::getSku, Product::getId));
        List<Inventory> rows = inventory.findAll();
        Map<Long, Integer> openingStock =
                rows.stream().collect(Collectors.toMap(Inventory::getId, Inventory::getAvailableQuantity));

        List<Event> events = new ArrayList<>();
        for (int i = 0; i < ORDERS.size(); i++) {
            addOrderEvents(events, i, ORDERS.get(i), customerIds, productIds, adminId);
        }
        for (RestockSeed restock : RESTOCKS) {
            long rowId = rowFor(rows, restock.sku(), restock.warehouse()).getId();
            events.add(new Event(at(restock.daysAgo(), 8), () -> inventoryService.update(
                    rowId,
                    new InventoryUpdateRequest(StockChangeType.RESTOCK, restock.quantity(), null, "Supplier delivery"),
                    adminId)));
        }

        events.stream().sorted(Comparator.comparing(Event::at)).forEach(event -> {
            clock.travelTo(event.at());
            event.action().run();
        });

        clock.resume();
        restoreOpeningStock(rows, openingStock, adminId);
        log.info("Created the demonstration history: {} customers, {} orders.", CUSTOMERS.size(), ORDERS.size());
    }

    private List<Long> createCustomers() {
        Instant joined = clock.instant();
        String hash = passwordEncoder.encode(customerPassword);
        return CUSTOMERS.stream()
                .map(seed -> users.save(new User(seed.name(), seed.email(), hash, seed.phone(), UserRole.CUSTOMER, joined)).getId())
                .toList();
    }

    private void addOrderEvents(
            List<Event> events,
            int index,
            OrderSeed seed,
            List<Long> customerIds,
            Map<String, Long> productIds,
            long adminId) {
        long customerId = customerIds.get(seed.customer());
        CustomerSeed customer = CUSTOMERS.get(seed.customer());
        // Minutes keep the times from all looking alike, and separate orders placed in the same hour.
        Instant placed = at(seed.daysAgo(), seed.hour()).plus(Duration.ofMinutes(index * 7L % 53));
        OrderCreateRequest request = new OrderCreateRequest(
                seed.lines().stream()
                        .map(line -> new OrderItemRequest(productIds.get(line.sku()), line.quantity()))
                        .toList(),
                customer.name(),
                customer.phone(),
                customer.address(),
                customer.city(),
                customer.pin(),
                seed.priority(),
                true);
        long[] orderId = new long[1];

        events.add(new Event(placed, () -> {
            OrderDetailResponse order = orderService.place(request, "seed-history-%03d".formatted(index), customerId);
            orderId[0] = order.id();
            boolean expectAllocation = seed.outcome() != Outcome.AWAITING_STOCK;
            if (expectAllocation != (order.status() == OrderStatus.CONFIRMED)) {
                throw new IllegalStateException(
                        "Seed order %d was expected to end %s but is %s".formatted(index, seed.outcome(), order.status()));
            }
        }));

        if (seed.outcome() == Outcome.CANCELLED) {
            events.add(new Event(placed.plus(Duration.ofHours(3)), () -> orderService.cancel(
                    orderId[0], new AuthenticatedUser(customerId, UserRole.CUSTOMER))));
            return;
        }
        for (Map.Entry<ShipmentStatus, Integer> step : shipmentSteps(seed).entrySet()) {
            events.add(new Event(placed.plus(Duration.ofHours(step.getValue())), () -> dispatchService.updateStatus(
                    dispatches.findByOrderId(orderId[0]).orElseThrow().getId(),
                    new DispatchStatusRequest(step.getKey(), null),
                    adminId)));
        }
    }

    /**
     * Hours after placing at which each shipment step happens. The timings keep Standard and Express
     * deliveries inside their estimates, except the late ones, and keep open shipments in the past.
     */
    private static Map<ShipmentStatus, Integer> shipmentSteps(OrderSeed seed) {
        return switch (seed.outcome()) {
            case DELIVERED -> seed.priority() == EXPRESS
                    ? steps(2, 6, 10, 20)
                    : steps(5, 20, 26, 44);
            case DELIVERED_LATE -> steps(24, 72, 96, 168);
            case IN_TRANSIT -> steps(2, 6, 10);
            case DISPATCHED -> steps(2, 6);
            case PACKED -> steps(2);
            default -> Map.of();
        };
    }

    private static Map<ShipmentStatus, Integer> steps(int... hours) {
        List<ShipmentStatus> order = List.of(
                ShipmentStatus.PACKED, ShipmentStatus.DISPATCHED, ShipmentStatus.IN_TRANSIT, ShipmentStatus.DELIVERED);
        Map<ShipmentStatus, Integer> steps = new LinkedHashMap<>();
        for (int i = 0; i < hours.length; i++) {
            steps.put(order.get(i), hours[i]);
        }
        return steps;
    }

    private static Inventory rowFor(List<Inventory> rows, String sku, String warehouse) {
        return rows.stream()
                .filter(row -> row.getProduct().getSku().equals(sku) && row.getWarehouse().getName().equals(warehouse))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No stock row for " + sku + " in " + warehouse));
    }

    private void restoreOpeningStock(List<Inventory> rows, Map<Long, Integer> openingStock, long adminId) {
        for (Inventory row : rows) {
            int change = openingStock.get(row.getId()) - row.getAvailableQuantity();
            if (change != 0) {
                inventoryService.update(
                        row.getId(),
                        new InventoryUpdateRequest(StockChangeType.ADJUSTMENT, change, null, STOCK_RESET_REASON),
                        adminId);
            }
        }
    }

    private static Instant at(int daysAgoFromToday, int hour) {
        return LocalDate.now(BUSINESS_ZONE)
                .minusDays(daysAgoFromToday)
                .atTime(hour, 0)
                .atZone(BUSINESS_ZONE)
                .toInstant();
    }
}
