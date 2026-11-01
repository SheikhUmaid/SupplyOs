package com.supplychain.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.supplychain.TestCatalog;
import com.supplychain.order.OrderDtos.OrderCreateRequest;
import com.supplychain.order.OrderDtos.OrderDetailResponse;
import com.supplychain.order.OrderDtos.OrderItemRequest;
import com.supplychain.product.Product;
import com.supplychain.user.User;
import com.supplychain.user.UserRepository;
import com.supplychain.user.UserRole;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Real threads and real commits. It cleans up after itself. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestCatalog.class)
class OrderIdempotencyConcurrencyTest {

    private static final int THREADS = 12;

    @Autowired
    private OrderService orderService;

    @Autowired
    private TestCatalog catalog;

    @Autowired
    private UserRepository users;

    @Autowired
    private JdbcTemplate jdbc;

    private Product product;
    private User customer;

    @AfterEach
    void cleanUp() {
        jdbc.update("""
                DELETE e FROM allocation_evaluations e
                JOIN warehouse_allocations a ON e.allocation_id = a.id
                JOIN orders o ON a.order_id = o.id WHERE o.customer_id = ?""", customer.getId());
        jdbc.update("""
                DELETE a FROM warehouse_allocations a
                JOIN orders o ON a.order_id = o.id WHERE o.customer_id = ?""", customer.getId());
        jdbc.update("DELETE i FROM order_items i JOIN orders o ON i.order_id = o.id WHERE o.customer_id = ?", customer.getId());
        jdbc.update("DELETE FROM orders WHERE customer_id = ?", customer.getId());
        jdbc.update("DELETE FROM users WHERE id = ?", customer.getId());
        jdbc.update("DELETE FROM products WHERE id = ?", product.getId());
        jdbc.update("DELETE FROM categories WHERE id = ?", product.getCategory().getId());
    }

    @Test
    void simultaneousSubmissionsWithOneKeyCreateOneOrder() throws Exception {
        product = catalog.product("Double click phone");
        customer = users.saveAndFlush(new User(
                "Customer", UUID.randomUUID() + "@example.com", "unused-hash", null, UserRole.CUSTOMER, Instant.now()));
        OrderCreateRequest request = new OrderCreateRequest(
                List.of(new OrderItemRequest(product.getId(), 1)),
                "Asha Rao",
                "9876543210",
                "12 MG Road",
                "Hubli",
                "580020",
                OrderPriority.STANDARD,
                true);

        CountDownLatch start = new CountDownLatch(1);
        List<Future<OrderDetailResponse>> results = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            for (int i = 0; i < THREADS; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return orderService.place(request, "same-key-for-all", customer.getId());
                }));
            }
            start.countDown();
            for (Future<OrderDetailResponse> result : results) {
                assertThat(result.get().orderNumber()).startsWith("ORD-");
            }
        }

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE customer_id = ?", Long.class, customer.getId()))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM order_items i JOIN orders o ON i.order_id = o.id WHERE o.customer_id = ?",
                        Long.class,
                        customer.getId()))
                .isEqualTo(1);
    }
}
