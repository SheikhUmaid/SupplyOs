package com.supplychain.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.supplychain.TestCatalog;
import com.supplychain.exception.ApiException;
import com.supplychain.inventory.InventoryDtos.InventoryUpdateRequest;
import com.supplychain.inventory.InventoryDtos.StockChangeType;
import com.supplychain.product.Product;
import com.supplychain.user.User;
import com.supplychain.user.UserRepository;
import com.supplychain.user.UserRole;
import com.supplychain.warehouse.Warehouse;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Real threads and real commits, so this can't use the usual per-test rollback. It cleans up after itself. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestCatalog.class)
class InventoryConcurrencyTest {

    private static final int THREADS = 16;

    @Autowired
    private InventoryService service;

    @Autowired
    private InventoryRepository inventory;

    @Autowired
    private TestCatalog catalog;

    @Autowired
    private UserRepository users;

    @Autowired
    private JdbcTemplate jdbc;

    private Product product;
    private Warehouse warehouse;
    private User admin;

    @BeforeEach
    void setUp() {
        product = catalog.product("Concurrent phone");
        warehouse = catalog.warehouse("Concurrent warehouse");
        admin = users.saveAndFlush(new User(
                "Admin", UUID.randomUUID() + "@example.com", "unused-hash", null, UserRole.ADMIN, Instant.now()));
    }

    @AfterEach
    void cleanUp() {
        jdbc.update(
                "DELETE t FROM inventory_transactions t JOIN inventory i ON t.inventory_id = i.id WHERE i.product_id = ?",
                product.getId());
        jdbc.update("DELETE FROM inventory WHERE product_id = ?", product.getId());
        jdbc.update("DELETE FROM products WHERE id = ?", product.getId());
        jdbc.update("DELETE FROM categories WHERE id = ?", product.getCategory().getId());
        jdbc.update("DELETE FROM warehouses WHERE id = ?", warehouse.getId());
        jdbc.update("DELETE FROM users WHERE id = ?", admin.getId());
    }

    private InventoryUpdateRequest change(StockChangeType type, int quantity) {
        return new InventoryUpdateRequest(type, quantity, null, "Concurrent test");
    }

    /** Runs the task on every thread at once and returns how many calls succeeded. */
    private int runTogether(Runnable task) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        task.run();
                        successes.incrementAndGet();
                    } catch (ApiException expectedRefusal) {
                        // A refused change is a valid outcome. Anything else fails the test below.
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        }
        return successes.get();
    }

    private long ledgerSum(long inventoryId) {
        return jdbc.queryForObject(
                "SELECT COALESCE(SUM(available_delta), 0) FROM inventory_transactions WHERE inventory_id = ?",
                Long.class,
                inventoryId);
    }

    @Test
    void simultaneousRestocksAreAllCounted() throws Exception {
        long id = catalog.stock(product, warehouse, 0, 0, 0).getId();

        int successes = runTogether(() -> service.update(id, change(StockChangeType.RESTOCK, 5), admin.getId()));

        assertThat(successes).isEqualTo(THREADS);
        assertThat(inventory.findById(id).orElseThrow().getAvailableQuantity()).isEqualTo(THREADS * 5);
        assertThat(ledgerSum(id)).isEqualTo(THREADS * 5);
    }

    /** The spec's case at the stock level: 10 units and competing removals of 7. Only one can win. */
    @Test
    void simultaneousRemovalsCannotOversell() throws Exception {
        long id = catalog.stock(product, warehouse, 10, 0, 0).getId();

        int successes = runTogether(() -> service.update(id, change(StockChangeType.ADJUSTMENT, -7), admin.getId()));

        assertThat(successes).isEqualTo(1);
        assertThat(inventory.findById(id).orElseThrow().getAvailableQuantity()).isEqualTo(3);
        assertThat(ledgerSum(id)).isEqualTo(-7);
    }

    @Test
    void stockAlwaysMatchesItsLedgerUnderMixedChanges() throws Exception {
        int opening = 20;
        long id = catalog.stock(product, warehouse, opening, 0, 0).getId();
        AtomicInteger counter = new AtomicInteger();

        runTogether(() -> {
            boolean add = counter.incrementAndGet() % 2 == 0;
            service.update(
                    id,
                    add ? change(StockChangeType.RESTOCK, 3) : change(StockChangeType.ADJUSTMENT, -4),
                    admin.getId());
        });

        int finalStock = inventory.findById(id).orElseThrow().getAvailableQuantity();
        assertThat(finalStock).isGreaterThanOrEqualTo(0);
        assertThat(finalStock).isEqualTo(opening + ledgerSum(id));
    }
}
