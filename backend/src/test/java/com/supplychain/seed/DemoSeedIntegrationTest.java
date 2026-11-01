package com.supplychain.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.supplychain.TestSessions;
import com.supplychain.config.AdjustableClock;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Runs the real seed on the test database, then rolls it back. */
@SpringBootTest(properties = {"app.seed.enabled=false", "SEED_CUSTOMER_PASSWORD=seed-test-password"})
@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@Transactional
@Import(TestSessions.class)
class DemoSeedIntegrationTest {

    @Autowired
    private CatalogSeeder seeder;

    @Autowired
    private AdjustableClock clock;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestSessions sessions;

    @Autowired
    private EntityManager entityManager;

    private Cookie admin;

    @BeforeEach
    void seed() {
        admin = sessions.admin();
        seeder.seed();
        // This test's transaction never commits, and the checks below read with plain SQL.
        entityManager.flush();
    }

    private long count(String sql) {
        return jdbc.queryForObject(sql, Long.class);
    }

    private Cookie loginAsAsha() throws Exception {
        return mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"asha.rao@demo.local\",\"password\":\"seed-test-password\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("scos_token"))
                .andReturn()
                .getResponse()
                .getCookie("scos_token");
    }

    private int available(String sku, String warehouse) {
        return jdbc.queryForObject(
                """
                SELECT i.available_quantity FROM inventory i
                JOIN products p ON p.id = i.product_id JOIN warehouses w ON w.id = i.warehouse_id
                WHERE p.sku = ? AND w.name = ?""",
                Integer.class,
                sku,
                warehouse);
    }

    @Test
    void itCreatesTheRequiredVolumes() {
        assertThat(count("SELECT COUNT(*) FROM warehouses")).isEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM products")).isBetween(10L, 15L);
        assertThat(count("SELECT COUNT(*) FROM users WHERE role = 'CUSTOMER'")).isEqualTo(10);
        assertThat(count("SELECT COUNT(*) FROM orders")).isGreaterThanOrEqualTo(15);
    }

    @Test
    void everyCustomerHasOrdersAndAStoredPasswordHash() {
        assertThat(count("SELECT COUNT(DISTINCT customer_id) FROM orders")).isEqualTo(10);
        assertThat(jdbc.queryForList("SELECT password_hash FROM users WHERE role = 'CUSTOMER'", String.class))
                .allSatisfy(hash -> assertThat(hash).doesNotContain("seed-test-password").startsWith("$2"));
    }

    @Test
    void everyShipmentStatusIsPresent() {
        assertThat(jdbc.queryForList("SELECT DISTINCT status FROM dispatches", String.class))
                .containsExactlyInAnyOrder("PENDING", "PACKED", "DISPATCHED", "IN_TRANSIT", "DELIVERED", "CANCELLED");
    }

    @Test
    void anOrderNobodyCanFillIsWaitingWithoutADispatch() {
        assertThat(count("""
                SELECT COUNT(*) FROM orders o
                WHERE o.status = 'PENDING' AND o.selected_warehouse_id IS NULL
                  AND NOT EXISTS (SELECT 1 FROM dispatches d WHERE d.order_id = o.id)"""))
                .isEqualTo(1);
    }

    @Test
    void everyOrderHasAStoredDecisionFromTheEngine() {
        assertThat(count("""
                SELECT COUNT(*) FROM orders o
                WHERE NOT EXISTS (SELECT 1 FROM warehouse_allocations a WHERE a.order_id = o.id)"""))
                .isZero();
    }

    @Test
    void historyIsSpreadOverRealPastDatesAndTheClockIsBackToNow() {
        assertThat(count("SELECT DATEDIFF(MAX(created_at), MIN(created_at)) FROM orders")).isGreaterThan(45);
        assertThat(count("SELECT COUNT(*) FROM orders WHERE created_at > UTC_TIMESTAMP(6)")).isZero();
        assertThat(count("SELECT COUNT(*) FROM delivery_tracking WHERE created_at > UTC_TIMESTAMP(6)")).isZero();
        assertThat(count("SELECT COUNT(*) FROM inventory_transactions WHERE created_at > UTC_TIMESTAMP(6)")).isZero();
        assertThat(Duration.between(clock.instant(), Instant.now()).abs()).isLessThan(Duration.ofMinutes(1));
    }

    @Test
    void trackingFollowsEachOrderInTime() {
        assertThat(count("""
                SELECT COUNT(*) FROM delivery_tracking t
                JOIN dispatches d ON d.id = t.dispatch_id JOIN orders o ON o.id = d.order_id
                WHERE t.created_at < o.created_at"""))
                .isZero();
    }

    @Test
    void theLedgerExplainsEveryStockFigure() {
        assertThat(count("""
                SELECT COUNT(*) FROM inventory i
                LEFT JOIN (SELECT inventory_id, SUM(available_delta) a, SUM(reserved_delta) r
                           FROM inventory_transactions GROUP BY inventory_id) l ON l.inventory_id = i.id
                WHERE COALESCE(l.a, 0) <> i.available_quantity OR COALESCE(l.r, 0) <> i.reserved_quantity"""))
                .isZero();
    }

    @Test
    void theLedgerRunsInTimeOrderWithConsistentRunningTotals() {
        // Each row's running total must equal the previous row's plus its own change.
        assertThat(count("""
                SELECT COUNT(*) FROM (
                  SELECT available_after, available_delta, created_at,
                         LAG(available_after) OVER w AS previous, LAG(created_at) OVER w AS previous_at
                  FROM inventory_transactions
                  WINDOW w AS (PARTITION BY inventory_id ORDER BY id)) ledger
                WHERE previous IS NOT NULL
                  AND (previous + available_delta <> available_after OR created_at < previous_at)"""))
                .isZero();
    }

    @Test
    void reservedStockMatchesOrdersThatHaveNotLeft() {
        assertThat(count("SELECT COALESCE(SUM(reserved_quantity), 0) FROM inventory")).isEqualTo(count("""
                SELECT COALESCE(SUM(oi.quantity), 0) FROM order_items oi
                JOIN dispatches d ON d.order_id = oi.order_id
                WHERE d.status IN ('PENDING', 'PACKED')"""));
    }

    @Test
    void stockEndsAtTheValuesTheScenariosNeed() {
        assertThat(List.of(
                        available("PH-1001", "Hubli Warehouse"),
                        available("PH-1001", "Bengaluru Warehouse"),
                        available("PH-1001", "Mysuru Warehouse")))
                .containsExactly(15, 50, 5);
        assertThat(List.of(
                        available("AU-2001", "Hubli Warehouse"),
                        available("AU-2001", "Bengaluru Warehouse"),
                        available("AU-2001", "Mysuru Warehouse")))
                .containsExactly(10, 30, 20);
        assertThat(List.of(
                        available("CO-3002", "Hubli Warehouse"),
                        available("CO-3002", "Bengaluru Warehouse"),
                        available("CO-3002", "Mysuru Warehouse")))
                .containsExactly(4, 4, 4);
        assertThat(count("""
                SELECT COUNT(*) FROM inventory_transactions
                WHERE type = 'ADJUSTMENT' AND note = 'Set to demonstration stock level'"""))
                .isPositive();
    }

    @Test
    void severalItemsAreLowOnStock() {
        assertThat(count("SELECT COUNT(*) FROM inventory WHERE available_quantity < reorder_level"))
                .isGreaterThanOrEqualTo(3);
    }

    @Test
    void theRequiredScenarioStillPicksHubliAndRejectsMysuru() throws Exception {
        Cookie asha = loginAsAsha();
        String phone = jdbc.queryForObject("SELECT id FROM products WHERE sku = 'PH-1001'", String.class);
        String headphones = jdbc.queryForObject("SELECT id FROM products WHERE sku = 'AU-2001'", String.class);

        String placed = mvc.perform(post("/api/orders")
                        .cookie(asha)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"productId":%s,"quantity":10},{"productId":%s,"quantity":5}],
                                 "deliveryName":"Asha Rao","deliveryPhone":"9845000101","deliveryAddress":"14 PJ Extension",
                                 "deliveryCity":"Davanagere","deliveryPostalCode":"577002","priority":"STANDARD",
                                 "simulatedPaymentConfirmed":true}""".formatted(phone, headphones)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.selectedWarehouse").value("Hubli Warehouse"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = Long.parseLong(placed.replaceAll("^\\{\"id\":(\\d+),.*$", "$1"));

        String decision = mvc.perform(get("/api/allocations/" + id).cookie(admin))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(decision).contains("Insufficient stock of Smartphone").contains("0.4746");
    }

    @Test
    void aCustomerSeesOnlyTheirOwnSeededOrders() throws Exception {
        Cookie asha = loginAsAsha();
        long own = count("""
                SELECT COUNT(*) FROM orders o JOIN users u ON u.id = o.customer_id
                WHERE u.email = 'asha.rao@demo.local'""");
        long other = count("""
                SELECT MIN(o.id) FROM orders o JOIN users u ON u.id = o.customer_id
                WHERE u.email <> 'asha.rao@demo.local'""");

        mvc.perform(get("/api/orders/my-orders").cookie(asha)).andExpect(jsonPath("$.totalElements").value(own));
        mvc.perform(get("/api/orders/" + other).cookie(asha)).andExpect(status().isNotFound());
    }

    @Test
    void theDashboardReportsTheSeededHistory() throws Exception {
        mvc.perform(get("/api/analytics/orders").cookie(admin).param("days", "90"))
                .andExpect(jsonPath("$.totalOrders").value(count("SELECT COUNT(*) FROM orders")))
                .andExpect(jsonPath("$.ordersByStatus.length()").value(6));
        mvc.perform(get("/api/analytics/dispatches").cookie(admin).param("days", "90"))
                .andExpect(jsonPath("$.statusDistribution.length()").value(6));
    }

    @Test
    void runningAgainChangesNothing() {
        long orders = count("SELECT COUNT(*) FROM orders");
        long ledger = count("SELECT COUNT(*) FROM inventory_transactions");

        seeder.seed();

        assertThat(count("SELECT COUNT(*) FROM orders")).isEqualTo(orders);
        assertThat(count("SELECT COUNT(*) FROM inventory_transactions")).isEqualTo(ledger);
    }
}
