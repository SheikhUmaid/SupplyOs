package com.supplychain.allocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import com.supplychain.TestCatalog;
import com.supplychain.inventory.Inventory;
import com.supplychain.inventory.InventoryDtos.InventoryUpdateRequest;
import com.supplychain.inventory.InventoryDtos.StockChangeType;
import com.supplychain.inventory.InventoryRepository;
import com.supplychain.inventory.InventoryService;
import com.supplychain.inventory.InventoryTransactionRepository;
import com.supplychain.inventory.InventoryTransactionType;
import com.supplychain.order.OrderDtos.OrderCreateRequest;
import com.supplychain.order.OrderDtos.OrderDetailResponse;
import com.supplychain.order.OrderDtos.OrderItemRequest;
import com.supplychain.order.OrderPriority;
import com.supplychain.order.OrderService;
import com.supplychain.order.OrderStatus;
import com.supplychain.product.Product;
import com.supplychain.user.User;
import com.supplychain.user.UserRepository;
import com.supplychain.user.UserRole;
import com.supplychain.warehouse.Warehouse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Real threads and real commits against MySQL, proving the spec's guarantee: stock can never be
 * oversold. This can't use the usual per-test rollback, so it empties the test database afterwards.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestCatalog.class)
class AllocationConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private TestCatalog catalog;

    @Autowired
    private InventoryRepository inventory;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private UserRepository users;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoSpyBean
    private InventoryTransactionRepository ledger;

    private User customer;

    @BeforeEach
    void createCustomer() {
        customer = users.saveAndFlush(new User(
                "Customer", UUID.randomUUID() + "@example.com", "unused-hash", null, UserRole.CUSTOMER, Instant.now()));
    }

    @AfterEach
    void emptyTheTestDatabase() {
        reset(ledger);
        for (String table : List.of(
                "delivery_tracking", "dispatches", "allocation_evaluations", "warehouse_allocations",
                "inventory_transactions", "order_items", "orders", "inventory", "products", "categories",
                "warehouses", "users")) {
            jdbc.update("DELETE FROM " + table);
        }
    }

    private OrderCreateRequest request(OrderItemRequest... items) {
        return new OrderCreateRequest(
                List.of(items), "Asha Rao", "9876543210", "12 MG Road", "Hubli", "580020", OrderPriority.STANDARD, true);
    }

    private OrderDetailResponse place(OrderItemRequest... items) {
        return orderService.place(request(items), UUID.randomUUID().toString(), customer.getId());
    }

    private <T> List<T> runTogether(List<Callable<T>> tasks) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        List<T> results = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(tasks.size())) {
            for (Callable<T> task : tasks) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            for (Future<T> future : futures) {
                results.add(future.get());
            }
        }
        return results;
    }

    private Inventory reload(Inventory row) {
        return inventory.findById(row.getId()).orElseThrow();
    }

    /** Stock is never created or lost: every row must agree with its ledger, and its opening balance. */
    private void assertLedgerAgrees(Inventory row, int openingAvailable) {
        Inventory current = reload(row);
        Long availableDeltas = jdbc.queryForObject(
                "SELECT COALESCE(SUM(available_delta), 0) FROM inventory_transactions WHERE inventory_id = ?",
                Long.class, row.getId());
        Long reservedDeltas = jdbc.queryForObject(
                "SELECT COALESCE(SUM(reserved_delta), 0) FROM inventory_transactions WHERE inventory_id = ?",
                Long.class, row.getId());
        assertThat(current.getAvailableQuantity()).isEqualTo(openingAvailable + availableDeltas);
        assertThat((long) current.getReservedQuantity()).isEqualTo(reservedDeltas);
        assertThat(current.getAvailableQuantity() + current.getReservedQuantity()).isEqualTo(openingAvailable);
        assertThat(current.getAvailableQuantity()).isNotNegative();
    }

    /** The spec's exact example: 10 in stock, two customers order 7 at once. */
    @Test
    void twoOrdersOfSevenFromStockOfTenCannotBothSucceed() throws Exception {
        Product phone = catalog.product("Phone");
        Inventory stock = catalog.stock(phone, catalog.warehouse("Only"), 10, 0, 0);
        OrderItemRequest seven = new OrderItemRequest(phone.getId(), 7);

        List<OrderDetailResponse> orders = runTogether(List.of(() -> place(seven), () -> place(seven)));

        assertThat(orders).extracting(OrderDetailResponse::status)
                .containsExactlyInAnyOrder(OrderStatus.CONFIRMED, OrderStatus.PENDING);
        Inventory current = reload(stock);
        assertThat(current.getAvailableQuantity()).isEqualTo(3);
        assertThat(current.getReservedQuantity()).isEqualTo(7);
        assertLedgerAgrees(stock, 10);
    }

    @Test
    void theLoserMovesToAnotherWarehouseWhenOneCanFillIt() throws Exception {
        Product phone = catalog.product("Phone");
        Warehouse first = catalog.warehouse("First");
        Warehouse second = catalog.warehouse("Second");
        Inventory firstStock = catalog.stock(phone, first, 10, 0, 0);
        Inventory secondStock = catalog.stock(phone, second, 10, 0, 0);
        OrderItemRequest seven = new OrderItemRequest(phone.getId(), 7);

        List<OrderDetailResponse> orders = runTogether(List.of(() -> place(seven), () -> place(seven)));

        assertThat(orders).extracting(OrderDetailResponse::status)
                .containsExactly(OrderStatus.CONFIRMED, OrderStatus.CONFIRMED);
        assertThat(orders).extracting(OrderDetailResponse::selectedWarehouse)
                .containsExactlyInAnyOrder(first.getName(), second.getName());
        assertThat(reload(firstStock).getReservedQuantity()).isEqualTo(7);
        assertThat(reload(secondStock).getReservedQuantity()).isEqualTo(7);
    }

    @Test
    void manyOrdersNeverOversell() throws Exception {
        Product phone = catalog.product("Phone");
        Inventory stock = catalog.stock(phone, catalog.warehouse("Only"), 25, 0, 0);
        OrderItemRequest three = new OrderItemRequest(phone.getId(), 3);

        List<Callable<OrderDetailResponse>> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tasks.add(() -> place(three));
        }
        List<OrderDetailResponse> orders = runTogether(tasks);

        // 25 units cover exactly eight orders of 3.
        assertThat(orders.stream().filter(order -> order.status() == OrderStatus.CONFIRMED)).hasSize(8);
        assertThat(orders.stream().filter(order -> order.status() == OrderStatus.PENDING)).hasSize(12);
        assertThat(reload(stock).getAvailableQuantity()).isEqualTo(1);
        assertThat(reload(stock).getReservedQuantity()).isEqualTo(24);
        assertLedgerAgrees(stock, 25);
    }

    @Test
    void ordersListingProductsInOppositeOrderDoNotDeadlock() throws Exception {
        Product a = catalog.product("A");
        Product b = catalog.product("B");
        Warehouse warehouse = catalog.warehouse("Only");
        Inventory stockA = catalog.stock(a, warehouse, 100, 0, 0);
        Inventory stockB = catalog.stock(b, warehouse, 100, 0, 0);

        List<Callable<OrderDetailResponse>> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            OrderItemRequest lineA = new OrderItemRequest(a.getId(), 1);
            OrderItemRequest lineB = new OrderItemRequest(b.getId(), 1);
            tasks.add(i % 2 == 0 ? () -> place(lineA, lineB) : () -> place(lineB, lineA));
        }
        List<OrderDetailResponse> orders = runTogether(tasks);

        assertThat(orders).allMatch(order -> order.status() == OrderStatus.CONFIRMED);
        assertThat(reload(stockA).getReservedQuantity()).isEqualTo(20);
        assertThat(reload(stockB).getReservedQuantity()).isEqualTo(20);
        assertLedgerAgrees(stockA, 100);
        assertLedgerAgrees(stockB, 100);
    }

    @Test
    void simultaneousCancelsReleaseStockOnce() throws Exception {
        Product phone = catalog.product("Phone");
        Inventory stock = catalog.stock(phone, catalog.warehouse("Only"), 10, 0, 0);
        long id = place(new OrderItemRequest(phone.getId(), 7)).id();
        com.supplychain.security.AuthenticatedUser owner =
                new com.supplychain.security.AuthenticatedUser(customer.getId(), UserRole.CUSTOMER);

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tasks.add(() -> {
                try {
                    orderService.cancel(id, owner);
                    return true;
                } catch (com.supplychain.exception.ApiException refused) {
                    return false;
                }
            });
        }
        List<Boolean> results = runTogether(tasks);

        assertThat(results.stream().filter(succeeded -> succeeded)).hasSize(1);
        assertThat(reload(stock).getAvailableQuantity()).isEqualTo(10);
        assertThat(reload(stock).getReservedQuantity()).isZero();
        assertLedgerAgrees(stock, 10);
    }

    @Test
    void adminRestocksDuringOrderingKeepTheBooksStraight() throws Exception {
        Product phone = catalog.product("Phone");
        Inventory stock = catalog.stock(phone, catalog.warehouse("Only"), 50, 0, 0);
        User admin = users.saveAndFlush(new User(
                "Admin", UUID.randomUUID() + "@example.com", "unused-hash", null, UserRole.ADMIN, Instant.now()));
        OrderItemRequest two = new OrderItemRequest(phone.getId(), 2);
        InventoryUpdateRequest addOne = new InventoryUpdateRequest(StockChangeType.RESTOCK, 1, null, "Delivery");

        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            tasks.add(() -> place(two));
            tasks.add(() -> inventoryService.update(stock.getId(), addOne, admin.getId()));
        }
        runTogether(tasks);

        Inventory current = reload(stock);
        assertThat(current.getReservedQuantity()).isEqualTo(24);
        assertThat(current.getAvailableQuantity()).isEqualTo(50 - 24 + 12);
        Long deltas = jdbc.queryForObject(
                "SELECT SUM(available_delta) FROM inventory_transactions WHERE inventory_id = ?", Long.class, stock.getId());
        assertThat(50 + deltas).isEqualTo(current.getAvailableQuantity());
    }

    /** A failure at any point must undo everything, leaving no order, decision or stock change behind. */
    @Test
    void aFailureDuringReservationRollsBackTheWholeOrder() {
        Product phone = catalog.product("Phone");
        Inventory stock = catalog.stock(phone, catalog.warehouse("Only"), 10, 0, 0);
        doThrow(new IllegalStateException("Simulated failure"))
                .when(ledger)
                .save(argThat(tx -> tx != null && tx.getType() == InventoryTransactionType.RESERVE));

        assertThatThrownBy(() -> place(new OrderItemRequest(phone.getId(), 7)))
                .hasMessageContaining("Simulated failure");

        assertThat(reload(stock).getAvailableQuantity()).isEqualTo(10);
        assertThat(reload(stock).getReservedQuantity()).isZero();
        for (String table : List.of("orders", "order_items", "warehouse_allocations", "allocation_evaluations",
                "inventory_transactions")) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class)).as(table).isZero();
        }
    }
}
