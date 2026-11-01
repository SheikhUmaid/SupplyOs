package com.supplychain.allocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.supplychain.IntegrationTest;
import com.supplychain.TestCatalog;
import com.supplychain.TestSessions;
import com.supplychain.inventory.Inventory;
import com.supplychain.inventory.InventoryRepository;
import com.supplychain.product.Product;
import com.supplychain.warehouse.Warehouse;
import com.supplychain.warehouse.WarehouseStatus;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** The required demonstration scenario: three warehouses, with Mysuru short of smartphones. */
@IntegrationTest
@Import({TestSessions.class, TestCatalog.class})
class AllocationIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestSessions sessions;

    @Autowired
    private TestCatalog catalog;

    @Autowired
    private InventoryRepository inventory;

    @Autowired
    private JdbcTemplate jdbc;

    private Warehouse hubli;
    private Warehouse bengaluru;
    private Warehouse mysuru;
    private Product phone;
    private Product headphones;
    private Inventory hubliPhones;
    private Inventory hubliHeadphones;
    private Inventory bengaluruPhones;
    private Cookie customer;

    @BeforeEach
    void setUpScenario() throws Exception {
        hubli = catalog.warehouse("Hubli", "15.3647", "75.1240", "120", "14", 12, WarehouseStatus.ACTIVE);
        bengaluru = catalog.warehouse("Bengaluru", "12.9716", "77.5946", "180", "16", 6, WarehouseStatus.ACTIVE);
        mysuru = catalog.warehouse("Mysuru", "12.2958", "76.6394", "100", "13", 18, WarehouseStatus.ACTIVE);
        phone = catalog.product("Smartphone");
        headphones = catalog.product("Wireless Headphones");
        hubliPhones = catalog.stock(phone, hubli, 15, 0, 5);
        bengaluruPhones = catalog.stock(phone, bengaluru, 50, 0, 5);
        catalog.stock(phone, mysuru, 5, 0, 5);
        hubliHeadphones = catalog.stock(headphones, hubli, 10, 0, 5);
        catalog.stock(headphones, bengaluru, 30, 0, 5);
        catalog.stock(headphones, mysuru, 20, 0, 5);
        customer = sessions.customer();
    }

    private String order(String city, String priority, String items) {
        return """
                {"items":%s,"deliveryName":"Asha Rao","deliveryPhone":"9876543210","deliveryAddress":"12 MG Road",
                 "deliveryCity":"%s","deliveryPostalCode":"577001","priority":"%s","simulatedPaymentConfirmed":true}
                """.formatted(items, city, priority);
    }

    private String demoItems() {
        return "[{\"productId\":%d,\"quantity\":10},{\"productId\":%d,\"quantity\":5}]"
                .formatted(phone.getId(), headphones.getId());
    }

    private ResultActions place(String json) throws Exception {
        return mvc.perform(post("/api/orders")
                .cookie(customer)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private long idOf(ResultActions result) throws Exception {
        String response = result.andReturn().getResponse().getContentAsString();
        return Long.parseLong(response.replaceAll("^\\{\"id\":(\\d+),.*$", "$1"));
    }

    private long placeDemo() throws Exception {
        return idOf(place(order("Davanagere", "STANDARD", demoItems())).andExpect(status().isCreated()));
    }

    private Inventory reload(Inventory row) {
        return inventory.findById(row.getId()).orElseThrow();
    }

    private void assertStock(Inventory row, int available, int reserved) {
        Inventory current = reload(row);
        assertThat(current.getAvailableQuantity()).as("available").isEqualTo(available);
        assertThat(current.getReservedQuantity()).as("reserved").isEqualTo(reserved);
    }

    @Test
    void theRequiredScenarioConfirmsTheOrderFromHubli() throws Exception {
        place(order("Davanagere", "STANDARD", demoItems()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.paymentStatus").value("PAID_SIMULATED"))
                .andExpect(jsonPath("$.selectedWarehouse").value(hubli.getName()))
                .andExpect(jsonPath("$.confirmedAt").isNotEmpty());
    }

    @Test
    void stockMovesFromAvailableToReservedAtTheSelectedWarehouseOnly() throws Exception {
        placeDemo();

        assertStock(hubliPhones, 5, 10);
        assertStock(hubliHeadphones, 5, 5);
        assertStock(bengaluruPhones, 50, 0);
    }

    @Test
    void reservationsAreRecordedInTheLedgerAgainstTheOrder() throws Exception {
        long id = placeDemo();

        List<java.util.Map<String, Object>> rows = jdbc.queryForList("""
                SELECT type, available_delta, reserved_delta, available_after, reserved_after, performed_by
                FROM inventory_transactions WHERE order_id = ? ORDER BY id""", id);
        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst())
                .containsEntry("type", "RESERVE")
                .containsEntry("available_delta", -10)
                .containsEntry("reserved_delta", 10)
                .containsEntry("available_after", 5)
                .containsEntry("reserved_after", 10)
                .containsEntry("performed_by", null);
    }

    @Test
    void theDecisionIsStoredAndExplainable() throws Exception {
        long id = placeDemo();
        Cookie admin = sessions.admin();

        mvc.perform(get("/api/allocations/" + id).cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptNo").value(1))
                .andExpect(jsonPath("$.outcome").value("ALLOCATED"))
                .andExpect(jsonPath("$.selectedWarehouse").value(hubli.getName()))
                .andExpect(jsonPath("$.weights.distance").value(0.4))
                .andExpect(jsonPath("$.weights.priority").value(0.1))
                .andExpect(jsonPath("$.explanation", startsWith(hubli.getName() + " was selected")))
                .andExpect(jsonPath("$.explanation", containsString("insufficient stock of Smartphone (needs 10, has 5)")))
                .andExpect(jsonPath("$.evaluations.length()").value(3))
                .andExpect(jsonPath("$.evaluations[0].warehouseName").value(hubli.getName()))
                .andExpect(jsonPath("$.evaluations[0].decision").value("SELECTED"))
                .andExpect(jsonPath("$.evaluations[0].rank").value(1))
                .andExpect(jsonPath("$.evaluations[0].finalScore", closeTo(0.4746, 0.0001)))
                .andExpect(jsonPath("$.evaluations[0].distanceKm", closeTo(164.7, 0.05)))
                .andExpect(jsonPath("$.evaluations[1].warehouseName").value(bengaluru.getName()))
                .andExpect(jsonPath("$.evaluations[1].decision").value("NOT_SELECTED"))
                .andExpect(jsonPath("$.evaluations[1].finalScore", closeTo(0.7367, 0.0001)))
                .andExpect(jsonPath("$.evaluations[2].warehouseName").value(mysuru.getName()))
                .andExpect(jsonPath("$.evaluations[2].decision").value("REJECTED"))
                .andExpect(jsonPath("$.evaluations[2].eligible").value(false))
                .andExpect(jsonPath("$.evaluations[2].finalScore").isEmpty())
                .andExpect(jsonPath("$.evaluations[2].distanceKm").isNumber());

        mvc.perform(get("/api/allocations/" + id + "/evaluations").cookie(admin))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void anotherCityChoosesAnotherWarehouse() throws Exception {
        place(order("Tumakuru", "STANDARD", demoItems()))
                .andExpect(jsonPath("$.selectedWarehouse").value(bengaluru.getName()));
        assertStock(bengaluruPhones, 40, 10);
    }

    @Test
    void reservedStockIsNotAvailableToTheNextOrder() throws Exception {
        placeDemo(); // Hubli now has 5 smartphones and 5 headphones left.

        long second = placeDemo();

        mvc.perform(get("/api/orders/" + second).cookie(customer))
                .andExpect(jsonPath("$.selectedWarehouse").value(bengaluru.getName()));
        mvc.perform(get("/api/allocations/" + second + "/evaluations").cookie(sessions.admin()))
                .andExpect(jsonPath("$[1].rejectionReason", containsString("Smartphone (needs 10, has 5)")));
    }

    @Test
    void anInactiveWarehouseIsPassedOverAndTheRecordSaysWhy() throws Exception {
        hubli.setStatus(WarehouseStatus.INACTIVE);
        catalog.save(hubli);

        long id = placeDemo();

        mvc.perform(get("/api/orders/" + id).cookie(customer))
                .andExpect(jsonPath("$.selectedWarehouse").value(bengaluru.getName()));
        mvc.perform(get("/api/allocations/" + id).cookie(sessions.admin()))
                .andExpect(jsonPath("$.explanation", containsString("The warehouse is inactive".toLowerCase())));
        assertStock(hubliPhones, 15, 0);
    }

    // No eligible warehouse

    private String tooManyPhones() {
        return "[{\"productId\":%d,\"quantity\":51}]".formatted(phone.getId());
    }

    @Test
    void anOrderNoWarehouseCanFillStaysPendingAndUnpaidWithNothingReserved() throws Exception {
        long id = idOf(place(order("Davanagere", "STANDARD", tooManyPhones()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentStatus").value("UNPAID"))
                .andExpect(jsonPath("$.selectedWarehouse").isEmpty()));

        assertStock(hubliPhones, 15, 0);
        assertStock(bengaluruPhones, 50, 0);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM inventory_transactions WHERE order_id = ?", Long.class, id))
                .isZero();
        mvc.perform(get("/api/allocations/" + id).cookie(sessions.admin()))
                .andExpect(jsonPath("$.outcome").value("NO_ELIGIBLE_WAREHOUSE"))
                .andExpect(jsonPath("$.selectedWarehouse").isEmpty())
                .andExpect(jsonPath("$.evaluations[0].decision").value("REJECTED"))
                .andExpect(jsonPath("$.evaluations[1].decision").value("REJECTED"))
                .andExpect(jsonPath("$.evaluations[2].decision").value("REJECTED"));
    }

    @Test
    void ordersAreNeverSplitAcrossWarehouses() throws Exception {
        // 70 smartphones exist in total, but no single warehouse has 60.
        String items = "[{\"productId\":%d,\"quantity\":60}]".formatted(phone.getId());
        place(order("Davanagere", "STANDARD", items)).andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void aProductMissingFromEveryWarehouseStaysPending() throws Exception {
        Product unstocked = catalog.product("Unstocked");
        String items = "[{\"productId\":%d,\"quantity\":1}]".formatted(unstocked.getId());
        place(order("Davanagere", "STANDARD", items)).andExpect(jsonPath("$.status").value("PENDING"));
    }

    // Re-running allocation

    @Test
    void aPendingOrderCanBeAllocatedAfterRestocking() throws Exception {
        long id = idOf(place(order("Davanagere", "STANDARD", tooManyPhones())));
        Cookie admin = sessions.admin();

        mvc.perform(put("/api/inventory/" + bengaluruPhones.getId())
                        .cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"RESTOCK\",\"quantity\":10,\"reason\":\"Delivery\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/orders/" + id + "/reallocate").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.paymentStatus").value("PAID_SIMULATED"))
                .andExpect(jsonPath("$.selectedWarehouse").value(bengaluru.getName()));

        assertStock(bengaluruPhones, 9, 51);
        mvc.perform(get("/api/allocations/" + id).cookie(admin))
                .andExpect(jsonPath("$.attemptNo").value(2))
                .andExpect(jsonPath("$.outcome").value("ALLOCATED"));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM warehouse_allocations WHERE order_id = ?", Long.class, id))
                .as("every attempt is kept")
                .isEqualTo(2);
    }

    @Test
    void reRunningWithoutNewStockRecordsAnotherAttemptAndStaysPending() throws Exception {
        long id = idOf(place(order("Davanagere", "STANDARD", tooManyPhones())));

        mvc.perform(post("/api/orders/" + id + "/reallocate").cookie(sessions.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        assertStock(bengaluruPhones, 50, 0);
    }

    @Test
    void aConfirmedOrderCannotBeAllocatedAgain() throws Exception {
        long id = placeDemo();

        mvc.perform(post("/api/orders/" + id + "/reallocate").cookie(sessions.admin()))
                .andExpect(status().isConflict());

        assertStock(hubliPhones, 5, 10);
    }

    @Test
    void onlyAdministratorsCanReRunOrReadDecisions() throws Exception {
        long id = placeDemo();

        mvc.perform(post("/api/orders/" + id + "/reallocate").cookie(customer)).andExpect(status().isForbidden());
        mvc.perform(get("/api/allocations/" + id).cookie(customer)).andExpect(status().isForbidden());
        mvc.perform(get("/api/allocations").cookie(customer)).andExpect(status().isForbidden());
        mvc.perform(get("/api/settings/allocation").cookie(customer)).andExpect(status().isForbidden());
        mvc.perform(get("/api/allocations/" + id)).andExpect(status().isUnauthorized());
    }

    @Test
    void decisionsCanBeListedAndFiltered() throws Exception {
        long id = idOf(place(order("Davanagere", "STANDARD", tooManyPhones())));

        mvc.perform(get("/api/allocations").cookie(sessions.admin()).param("outcome", "NO_ELIGIBLE_WAREHOUSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value(id))
                .andExpect(jsonPath("$.content[0].selectedWarehouse").isEmpty());
    }

    @Test
    void anOrderWithoutADecisionIsNotFound() throws Exception {
        mvc.perform(get("/api/allocations/999999999").cookie(sessions.admin())).andExpect(status().isNotFound());
    }

    @Test
    void theActiveWeightsCanBeRead() throws Exception {
        mvc.perform(get("/api/settings/allocation").cookie(sessions.admin()))
                .andExpect(jsonPath("$.weights.distance").value(0.4))
                .andExpect(jsonPath("$.weights.cost").value(0.3))
                .andExpect(jsonPath("$.weights.inventory").value(0.2))
                .andExpect(jsonPath("$.weights.priority").value(0.1))
                .andExpect(jsonPath("$.roadFactor").value(1.25))
                .andExpect(jsonPath("$.speedKmh").value(45.0));
    }

    // Cancellation

    @Test
    void cancellingAConfirmedOrderReleasesItsStockAndRefunds() throws Exception {
        long id = placeDemo();
        assertStock(hubliPhones, 5, 10);

        mvc.perform(patch("/api/orders/" + id + "/cancel").cookie(customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.paymentStatus").value("REFUNDED_SIMULATED"))
                .andExpect(jsonPath("$.cancellable").value(false))
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty());

        assertStock(hubliPhones, 15, 0);
        assertStock(hubliHeadphones, 10, 0);
        List<String> types = jdbc.queryForList(
                "SELECT type FROM inventory_transactions WHERE order_id = ? ORDER BY id", String.class, id);
        assertThat(types).containsExactly("RESERVE", "RESERVE", "RELEASE", "RELEASE");
    }

    @Test
    void cancellingAPendingOrderTouchesNoStock() throws Exception {
        long id = idOf(place(order("Davanagere", "STANDARD", tooManyPhones())));

        mvc.perform(patch("/api/orders/" + id + "/cancel").cookie(customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.paymentStatus").value("UNPAID"));

        assertStock(bengaluruPhones, 50, 0);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM inventory_transactions WHERE order_id = ?", Long.class, id))
                .isZero();
    }

    @Test
    void anOrderCanOnlyBeCancelledOnceSoStockIsNotReleasedTwice() throws Exception {
        long id = placeDemo();
        mvc.perform(patch("/api/orders/" + id + "/cancel").cookie(customer)).andExpect(status().isOk());

        mvc.perform(patch("/api/orders/" + id + "/cancel").cookie(customer)).andExpect(status().isConflict());

        assertStock(hubliPhones, 15, 0);
    }

    @Test
    void aCancelledOrderCannotBeReallocated() throws Exception {
        long id = idOf(place(order("Davanagere", "STANDARD", tooManyPhones())));
        mvc.perform(patch("/api/orders/" + id + "/cancel").cookie(customer));

        mvc.perform(post("/api/orders/" + id + "/reallocate").cookie(sessions.admin()))
                .andExpect(status().isConflict());
    }

    @Test
    void administratorsCanCancelButOtherCustomersCannotEvenSeeTheOrder() throws Exception {
        long id = placeDemo();

        mvc.perform(patch("/api/orders/" + id + "/cancel").cookie(sessions.customer()))
                .andExpect(status().isNotFound());
        assertStock(hubliPhones, 5, 10);

        mvc.perform(patch("/api/orders/" + id + "/cancel").cookie(sessions.admin())).andExpect(status().isOk());
        assertStock(hubliPhones, 15, 0);
    }

    @Test
    void releasedStockCanBeAllocatedAgain() throws Exception {
        long first = placeDemo();
        mvc.perform(patch("/api/orders/" + first + "/cancel").cookie(customer));

        long second = placeDemo();

        mvc.perform(get("/api/orders/" + second).cookie(customer))
                .andExpect(jsonPath("$.selectedWarehouse").value(hubli.getName()));
    }

    @Test
    void repeatingAnOrderWithOneKeyReservesStockOnce() throws Exception {
        String json = order("Davanagere", "STANDARD", demoItems());
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/orders")
                            .cookie(customer)
                            .header("Idempotency-Key", "reserve-only-once")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated());
        }
        assertStock(hubliPhones, 5, 10);
    }
}
