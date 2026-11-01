package com.supplychain.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.supplychain.IntegrationTest;
import com.supplychain.TestCatalog;
import com.supplychain.TestSessions;
import com.supplychain.dispatch.DispatchRepository;
import com.supplychain.inventory.Inventory;
import com.supplychain.product.Product;
import com.supplychain.warehouse.Warehouse;
import com.supplychain.warehouse.WarehouseStatus;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** Every figure is checked against records this test created itself. */
@IntegrationTest
@Import({TestSessions.class, TestCatalog.class})
class AnalyticsIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestSessions sessions;

    @Autowired
    private TestCatalog catalog;

    @Autowired
    private DispatchRepository dispatches;

    @Autowired
    private JdbcTemplate jdbc;

    private Warehouse warehouse;
    private Product phone;
    private Product mouse;
    private Inventory phoneStock;
    private Cookie customer;
    private Cookie admin;

    @BeforeEach
    void setUp() {
        warehouse = catalog.warehouse("Hubli");
        phone = catalog.product("Phone"); // ₹999.50 each.
        mouse = catalog.product("Mouse");
        phoneStock = catalog.stock(phone, warehouse, 20, 0, 5);
        catalog.stock(mouse, warehouse, 2, 0, 5); // Below its reorder level.
        customer = sessions.customer();
        admin = sessions.admin();
    }

    private long order(Product product, int quantity) throws Exception {
        String response = mvc.perform(post("/api/orders")
                        .cookie(customer)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"productId":%d,"quantity":%d}],"deliveryName":"Asha Rao",
                                 "deliveryPhone":"9876543210","deliveryAddress":"12 MG Road","deliveryCity":"Hubli",
                                 "deliveryPostalCode":"580020","priority":"STANDARD","simulatedPaymentConfirmed":true}
                                """.formatted(product.getId(), quantity)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return Long.parseLong(response.replaceAll("^\\{\"id\":(\\d+),.*$", "$1"));
    }

    private void advance(long orderId, String... statuses) throws Exception {
        long dispatchId = dispatches.findByOrderId(orderId).orElseThrow().getId();
        for (String next : statuses) {
            mvc.perform(patch("/api/dispatches/" + dispatchId + "/status")
                            .cookie(admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"%s\"}".formatted(next)))
                    .andExpect(status().isOk());
        }
    }

    private void deliver(long orderId) throws Exception {
        advance(orderId, "PACKED", "DISPATCHED", "IN_TRANSIT", "DELIVERED");
    }

    private void cancel(long orderId) throws Exception {
        mvc.perform(patch("/api/orders/" + orderId + "/cancel").cookie(customer)).andExpect(status().isOk());
    }

    /** Moves an order back in time, as if it had been placed that many days ago. */
    private void backdate(long orderId, int days) {
        jdbc.update("UPDATE orders SET created_at = DATE_SUB(created_at, INTERVAL ? DAY) WHERE id = ?", days, orderId);
    }

    private static String today() {
        return LocalDate.now(ZoneId.of("Asia/Kolkata")).toString();
    }

    @Test
    void anEmptySystemReportsZerosRatherThanFailing() throws Exception {
        mvc.perform(get("/api/analytics/orders").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(0))
                .andExpect(jsonPath("$.averageOrderValue").value(0.0))
                .andExpect(jsonPath("$.allocationRate").isEmpty())
                .andExpect(jsonPath("$.averageDeliveryHours").isEmpty())
                .andExpect(jsonPath("$.topProducts.length()").value(0))
                .andExpect(jsonPath("$.ordersOverTime.length()").value(30));
        mvc.perform(get("/api/analytics/dispatches").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeDispatches").value(0))
                .andExpect(jsonPath("$.onTimeRate").isEmpty());
        mvc.perform(get("/api/analytics/dashboard").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.totalOrders").value(0));
    }

    @Test
    void theDashboardKpisMatchTheRecords() throws Exception {
        long delivered = order(phone, 1);
        deliver(delivered);
        order(phone, 2); // Confirmed, so its dispatch is active.
        order(phone, 50); // No warehouse can fill this, so it is pending.
        cancel(order(phone, 1));

        mvc.perform(get("/api/analytics/dashboard").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.totalOrders").value(4))
                .andExpect(jsonPath("$.kpis.pendingOrders").value(1))
                .andExpect(jsonPath("$.kpis.deliveredOrders").value(1))
                .andExpect(jsonPath("$.kpis.activeDispatches").value(1))
                .andExpect(jsonPath("$.kpis.totalProducts").value(2))
                .andExpect(jsonPath("$.kpis.totalWarehouses").value(1))
                // 22 opening units, less 1 shipped: 17 + 2 available, 2 reserved.
                .andExpect(jsonPath("$.kpis.totalInventory").value(21))
                .andExpect(jsonPath("$.kpis.lowStockItems").value(1));
    }

    @Test
    void theDashboardListsRecentActivityAndAlerts() throws Exception {
        long id = order(phone, 1);

        mvc.perform(get("/api/analytics/dashboard").cookie(admin))
                .andExpect(jsonPath("$.recentOrders.content[0].id").value(id))
                .andExpect(jsonPath("$.recentOrders.content[0].selectedWarehouse").value(warehouse.getName()))
                .andExpect(jsonPath("$.recentDispatches.content[0].orderId").value(id))
                .andExpect(jsonPath("$.inventoryAlerts.totalElements").value(1))
                .andExpect(jsonPath("$.inventoryAlerts.content[0].productName").value("Mouse"))
                .andExpect(jsonPath("$.orderActivity.length()").value(14))
                .andExpect(jsonPath("$.orderActivity[13].date").value(today()))
                .andExpect(jsonPath("$.orderActivity[13].count").value(1))
                .andExpect(jsonPath("$.orderActivity[12].count").value(0))
                .andExpect(jsonPath("$.warehousePerformance[0].allocatedOrders").value(1));
    }

    @Test
    void lowStockAgreesWithTheInventoryScreen() throws Exception {
        String api = mvc.perform(get("/api/inventory").cookie(admin).param("lowStockOnly", "true"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(api).contains("\"totalElements\":1");

        mvc.perform(get("/api/analytics/alerts").cookie(admin))
                .andExpect(jsonPath("$.lowStockItems").value(1))
                .andExpect(jsonPath("$.pendingOrders").value(0));
    }

    @Test
    void orderValueLeavesOutCancelledOrders() throws Exception {
        order(phone, 2); // ₹1,999.00.
        order(phone, 4); // ₹3,998.00.
        cancel(order(phone, 1));

        mvc.perform(get("/api/analytics/orders").cookie(admin))
                .andExpect(jsonPath("$.totalOrders").value(3))
                .andExpect(jsonPath("$.totalValue").value(5997.00))
                .andExpect(jsonPath("$.averageOrderValue").value(2998.50))
                .andExpect(jsonPath("$.cancellationRate", closeTo(0.3333, 0.001)));
    }

    @Test
    void theAllocationRateCountsOrdersThatFoundAWarehouse() throws Exception {
        order(phone, 1);
        order(phone, 50);

        mvc.perform(get("/api/analytics/orders").cookie(admin))
                .andExpect(jsonPath("$.allocationRate").value(0.5));
    }

    @Test
    void ordersAreBrokenDownByStatusBiggestFirst() throws Exception {
        order(phone, 1);
        order(phone, 1);
        order(phone, 50);

        mvc.perform(get("/api/analytics/orders").cookie(admin))
                .andExpect(jsonPath("$.ordersByStatus[0].key").value("CONFIRMED"))
                .andExpect(jsonPath("$.ordersByStatus[0].count").value(2))
                .andExpect(jsonPath("$.ordersByStatus[1].key").value("PENDING"))
                .andExpect(jsonPath("$.ordersByStatus[1].count").value(1));
    }

    @Test
    void theTimeSeriesFillsEmptyDaysAndCountsInIndianDays() throws Exception {
        long old = order(phone, 1);
        order(phone, 1);
        backdate(old, 3);

        mvc.perform(get("/api/analytics/orders").cookie(admin).param("days", "7"))
                .andExpect(jsonPath("$.ordersOverTime.length()").value(7))
                .andExpect(jsonPath("$.ordersOverTime[6].date").value(today()))
                .andExpect(jsonPath("$.ordersOverTime[6].count").value(1))
                .andExpect(jsonPath("$.ordersOverTime[5].count").value(0))
                .andExpect(jsonPath("$.ordersOverTime[3].count").value(1));
    }

    @Test
    void anOrderJustAfterMidnightInIndiaCountsForThatIndianDay() throws Exception {
        long id = order(phone, 1);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        // 00:10 IST today is 18:40 UTC the previous day.
        jdbc.update(
                "UPDATE orders SET created_at = ? WHERE id = ?",
                today.minusDays(1).atTime(18, 40),
                id);

        mvc.perform(get("/api/analytics/orders").cookie(admin).param("days", "1"))
                .andExpect(jsonPath("$.totalOrders").value(1))
                .andExpect(jsonPath("$.ordersOverTime[0].date").value(today.toString()))
                .andExpect(jsonPath("$.ordersOverTime[0].count").value(1));
    }

    @Test
    void thePeriodFilterExcludesOlderOrders() throws Exception {
        long old = order(phone, 1);
        order(phone, 1);
        backdate(old, 20);

        mvc.perform(get("/api/analytics/orders").cookie(admin).param("days", "7"))
                .andExpect(jsonPath("$.days").value(7))
                .andExpect(jsonPath("$.totalOrders").value(1));
        mvc.perform(get("/api/analytics/orders").cookie(admin).param("days", "30"))
                .andExpect(jsonPath("$.totalOrders").value(2));
        mvc.perform(get("/api/analytics/warehouses").cookie(admin).param("days", "7"))
                .andExpect(jsonPath("$.warehouses[0].allocatedOrders").value(1));
    }

    @Test
    void topProductsRankByUnitsAndIgnoreCancelledOrders() throws Exception {
        catalog.stock(mouse, catalog.warehouse("Second"), 50, 0, 0);
        order(phone, 2);
        order(mouse, 5);
        cancel(order(phone, 9));

        mvc.perform(get("/api/analytics/orders").cookie(admin))
                .andExpect(jsonPath("$.topProducts.length()").value(2))
                .andExpect(jsonPath("$.topProducts[0].name").value("Mouse"))
                .andExpect(jsonPath("$.topProducts[0].units").value(5))
                .andExpect(jsonPath("$.topProducts[1].name").value("Phone"))
                .andExpect(jsonPath("$.topProducts[1].units").value(2))
                .andExpect(jsonPath("$.topProducts[1].revenue").value(1999.00));
    }

    @Test
    void warehouseFiguresAndFulfilmentMatchTheirDefinition() throws Exception {
        deliver(order(phone, 1));
        order(phone, 2);
        cancel(order(phone, 1));

        mvc.perform(get("/api/analytics/warehouses").cookie(admin))
                .andExpect(jsonPath("$.warehouses[0].name").value(warehouse.getName()))
                .andExpect(jsonPath("$.warehouses[0].allocatedOrders").value(2))
                .andExpect(jsonPath("$.warehouses[0].completedDispatches").value(1))
                .andExpect(jsonPath("$.warehouses[0].fulfilmentRate").value(0.5))
                .andExpect(jsonPath("$.warehouses[0].availableUnits").value(19))
                .andExpect(jsonPath("$.warehouses[0].reservedUnits").value(2));
    }

    @Test
    void anInactiveWarehouseIsMarkedAndLeftOutOfStockFigures() throws Exception {
        Warehouse closed = catalog.warehouse("Closed", WarehouseStatus.INACTIVE);
        catalog.stock(phone, closed, 100, 0, 0);

        mvc.perform(get("/api/analytics/warehouses").cookie(admin))
                .andExpect(jsonPath("$.warehouses[0].active").value(true))
                .andExpect(jsonPath("$.warehouses[1].active").value(false));
        mvc.perform(get("/api/analytics/inventory").cookie(admin))
                .andExpect(jsonPath("$.warehouses.length()").value(1))
                .andExpect(jsonPath("$.totalAvailable").value(22));
    }

    @Test
    void inventoryAnalyticsReportStockAndLowStock() throws Exception {
        order(phone, 3);

        mvc.perform(get("/api/analytics/inventory").cookie(admin))
                .andExpect(jsonPath("$.totalAvailable").value(19))
                .andExpect(jsonPath("$.totalReserved").value(3))
                .andExpect(jsonPath("$.lowStockItems").value(1))
                .andExpect(jsonPath("$.lowStockProducts[0].productName").value("Mouse"))
                .andExpect(jsonPath("$.warehouses.length()").value(1))
                .andExpect(jsonPath("$.warehouses[0].id").value(warehouse.getId()))
                .andExpect(jsonPath("$.productAvailability[0].name").value("Phone"))
                .andExpect(jsonPath("$.productAvailability[0].totalAvailable").value(17))
                .andExpect(jsonPath("$.productAvailability[0].byWarehouse[0].warehouseId").value(warehouse.getId()))
                .andExpect(jsonPath("$.productAvailability[1].name").value("Mouse"));
    }

    @Test
    void stockMovementComesFromTheLedger() throws Exception {
        mvc.perform(put("/api/inventory/" + phoneStock.getId())
                        .cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"RESTOCK\",\"quantity\":6,\"reason\":\"Delivery\"}"))
                .andExpect(status().isOk());
        advance(order(phone, 3), "PACKED", "DISPATCHED");

        mvc.perform(get("/api/analytics/inventory").cookie(admin).param("days", "7"))
                .andExpect(jsonPath("$.stockMovement.length()").value(7))
                .andExpect(jsonPath("$.stockMovement[6].date").value(today()))
                .andExpect(jsonPath("$.stockMovement[6].received").value(6))
                .andExpect(jsonPath("$.stockMovement[6].shipped").value(3))
                .andExpect(jsonPath("$.stockMovement[5].received").value(0));
    }

    @Test
    void openingStockIsNotADelivery() throws Exception {
        Warehouse second = catalog.warehouse("Second");
        mvc.perform(post("/api/inventory")
                        .cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":%d,\"warehouseId\":%d,\"availableQuantity\":40,\"reorderLevel\":1}"
                                .formatted(phone.getId(), second.getId())))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/analytics/inventory").cookie(admin).param("days", "1"))
                .andExpect(jsonPath("$.totalAvailable").value(62))
                .andExpect(jsonPath("$.stockMovement[0].received").value(0));
    }

    @Test
    void dispatchAnalyticsCountEachStage() throws Exception {
        deliver(order(phone, 1));
        advance(order(phone, 1), "PACKED", "DISPATCHED");
        advance(order(phone, 1), "PACKED");
        order(phone, 1);
        cancel(order(phone, 1));

        mvc.perform(get("/api/analytics/dispatches").cookie(admin))
                .andExpect(jsonPath("$.activeDispatches").value(3))
                .andExpect(jsonPath("$.completedDeliveries").value(1))
                .andExpect(jsonPath("$.pendingShipments").value(2))
                .andExpect(jsonPath("$.onTimeRate").value(1.0))
                .andExpect(jsonPath("$.statusDistribution.length()").value(5));
    }

    @Test
    void aLateDeliveryLowersTheOnTimeRate() throws Exception {
        long onTime = order(phone, 1);
        deliver(onTime);
        long late = order(phone, 1);
        deliver(late);
        jdbc.update(
                "UPDATE dispatches SET estimated_delivery_date = DATE_SUB(CURDATE(), INTERVAL 5 DAY) WHERE order_id = ?",
                late);

        mvc.perform(get("/api/analytics/dispatches").cookie(admin))
                .andExpect(jsonPath("$.onTimeRate").value(0.5));
    }

    @Test
    void averageDeliveryTimeUsesRealTimestamps() throws Exception {
        long id = order(phone, 1);
        deliver(id);
        jdbc.update("UPDATE orders SET created_at = DATE_SUB(created_at, INTERVAL 10 HOUR) WHERE id = ?", id);

        mvc.perform(get("/api/analytics/orders").cookie(admin))
                .andExpect(jsonPath("$.averageDeliveryHours", closeTo(10.0, 0.1)));
    }

    @Test
    void anInvalidPeriodIsRefused() throws Exception {
        for (String path : List.of("orders", "warehouses", "inventory", "dispatches")) {
            mvc.perform(get("/api/analytics/" + path).cookie(admin).param("days", "0"))
                    .andExpect(status().isBadRequest());
            mvc.perform(get("/api/analytics/" + path).cookie(admin).param("days", "9999"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void analyticsAreForAdministratorsOnly() throws Exception {
        for (String path : List.of("dashboard", "alerts", "orders", "warehouses", "inventory", "dispatches")) {
            mvc.perform(get("/api/analytics/" + path).cookie(customer)).andExpect(status().isForbidden());
            mvc.perform(get("/api/analytics/" + path)).andExpect(status().isUnauthorized());
        }
    }
}
