package com.supplychain.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.supplychain.IntegrationTest;
import com.supplychain.TestCatalog;
import com.supplychain.TestSessions;
import com.supplychain.inventory.Inventory;
import com.supplychain.inventory.InventoryRepository;
import com.supplychain.product.Product;
import com.supplychain.warehouse.Warehouse;
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

@IntegrationTest
@Import({TestSessions.class, TestCatalog.class})
class DispatchIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestSessions sessions;

    @Autowired
    private TestCatalog catalog;

    @Autowired
    private InventoryRepository inventory;

    @Autowired
    private DispatchRepository dispatches;

    @Autowired
    private JdbcTemplate jdbc;

    private Product phone;
    private Warehouse warehouse;
    private Inventory stock;
    private Cookie customer;
    private Cookie admin;

    @BeforeEach
    void setUp() {
        phone = catalog.product("Phone");
        warehouse = catalog.warehouse("Hubli");
        stock = catalog.stock(phone, warehouse, 10, 0, 1);
        customer = sessions.customer();
        admin = sessions.admin();
    }

    private long placeOrder(int quantity) throws Exception {
        String response = mvc.perform(post("/api/orders")
                        .cookie(customer)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"productId":%d,"quantity":%d}],"deliveryName":"Asha Rao",
                                 "deliveryPhone":"9876543210","deliveryAddress":"12 MG Road","deliveryCity":"Hubli",
                                 "deliveryPostalCode":"580020","priority":"STANDARD","simulatedPaymentConfirmed":true}
                                """.formatted(phone.getId(), quantity)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return Long.parseLong(response.replaceAll("^\\{\"id\":(\\d+),.*$", "$1"));
    }

    private long dispatchIdOf(long orderId) {
        return dispatches.findByOrderId(orderId).orElseThrow().getId();
    }

    private ResultActions move(long dispatchId, String status) throws Exception {
        return mvc.perform(patch("/api/dispatches/" + dispatchId + "/status")
                .cookie(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"%s\",\"note\":\"Internal note\"}".formatted(status)));
    }

    private void moveTo(long dispatchId, String... statuses) throws Exception {
        for (String status : statuses) {
            move(dispatchId, status).andExpect(status().isOk());
        }
    }

    private void assertStock(int available, int reserved) {
        Inventory current = inventory.findById(stock.getId()).orElseThrow();
        assertThat(current.getAvailableQuantity()).as("available").isEqualTo(available);
        assertThat(current.getReservedQuantity()).as("reserved").isEqualTo(reserved);
    }

    private ResultActions orderShouldBe(long orderId, String status) throws Exception {
        return mvc.perform(get("/api/orders/" + orderId).cookie(customer))
                .andExpect(jsonPath("$.status").value(status));
    }

    @Test
    void aConfirmedOrderGetsADispatchAutomatically() throws Exception {
        long orderId = placeOrder(3);

        mvc.perform(get("/api/dispatches/" + dispatchIdOf(orderId)).cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.warehouseName").value(warehouse.getName()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.trackingReference", matchesPattern("^TRK-\\d{4}-\\d{6}$")))
                .andExpect(jsonPath("$.estimatedDeliveryDate").isNotEmpty())
                .andExpect(jsonPath("$.dispatchDate").isEmpty())
                .andExpect(jsonPath("$.deliveryDate").isEmpty())
                .andExpect(jsonPath("$.allowedNextStatuses[0]").value("PACKED"));
    }

    @Test
    void aPendingOrderHasNoDispatch() throws Exception {
        long orderId = placeOrder(50);

        assertThat(dispatches.findByOrderId(orderId)).isEmpty();
        mvc.perform(get("/api/tracking/order/" + orderId).cookie(customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipmentStatus").isEmpty())
                .andExpect(jsonPath("$.trackingReference").isEmpty())
                .andExpect(jsonPath("$.steps[0].completedAt").isNotEmpty())
                .andExpect(jsonPath("$.steps[1].completedAt").isEmpty())
                .andExpect(jsonPath("$.steps[2].completedAt").isEmpty());
    }

    @Test
    void reallocatingAPendingOrderCreatesItsDispatch() throws Exception {
        long orderId = placeOrder(50);
        jdbc.update("UPDATE inventory SET available_quantity = 60 WHERE id = ?", stock.getId());
        catalog.clearSession();

        mvc.perform(post("/api/orders/" + orderId + "/reallocate").cookie(admin))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        assertThat(dispatches.findByOrderId(orderId)).isPresent();
    }

    @Test
    void theOrderFollowsTheShipmentThroughItsWholeLifecycle() throws Exception {
        long orderId = placeOrder(3);
        long id = dispatchIdOf(orderId);
        orderShouldBe(orderId, "CONFIRMED");

        move(id, "PACKED").andExpect(status().isOk()).andExpect(jsonPath("$.allowedNextStatuses[0]").value("DISPATCHED"));
        orderShouldBe(orderId, "PROCESSING");

        move(id, "DISPATCHED").andExpect(status().isOk()).andExpect(jsonPath("$.dispatchDate").isNotEmpty());
        orderShouldBe(orderId, "DISPATCHED");

        move(id, "IN_TRANSIT").andExpect(status().isOk());
        orderShouldBe(orderId, "DISPATCHED");

        move(id, "DELIVERED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryDate").isNotEmpty())
                .andExpect(jsonPath("$.allowedNextStatuses.length()").value(0));
        orderShouldBe(orderId, "DELIVERED");
    }

    @Test
    void stockStaysReservedUntilTheShipmentLeavesThenLeavesTheBooks() throws Exception {
        long orderId = placeOrder(3);
        long id = dispatchIdOf(orderId);
        assertStock(7, 3);

        moveTo(id, "PACKED");
        assertStock(7, 3);

        moveTo(id, "DISPATCHED");
        assertStock(7, 0);

        moveTo(id, "IN_TRANSIT", "DELIVERED");
        assertStock(7, 0);

        List<String> types = jdbc.queryForList(
                "SELECT type FROM inventory_transactions WHERE order_id = ? ORDER BY id", String.class, orderId);
        assertThat(types).containsExactly("RESERVE", "SHIP");
        assertThat(jdbc.queryForMap("""
                        SELECT available_delta, reserved_delta, available_after, reserved_after
                        FROM inventory_transactions WHERE order_id = ? AND type = 'SHIP'""", orderId))
                .containsEntry("available_delta", 0)
                .containsEntry("reserved_delta", -3)
                .containsEntry("available_after", 7)
                .containsEntry("reserved_after", 0);
    }

    @Test
    void skippingAStepIsRefused() throws Exception {
        long id = dispatchIdOf(placeOrder(1));

        move(id, "DELIVERED")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("can only move to packed")));
        move(id, "DISPATCHED").andExpect(status().isConflict());
    }

    @Test
    void aShipmentCannotMoveBackwardsOrRepeatAStep() throws Exception {
        long orderId = placeOrder(1);
        long id = dispatchIdOf(orderId);
        moveTo(id, "PACKED");

        move(id, "PENDING").andExpect(status().isConflict());
        move(id, "PACKED").andExpect(status().isConflict());
    }

    @Test
    void aDeliveredShipmentIsFinal() throws Exception {
        long id = dispatchIdOf(placeOrder(1));
        moveTo(id, "PACKED", "DISPATCHED", "IN_TRANSIT", "DELIVERED");

        for (String status : List.of("PENDING", "PACKED", "DISPATCHED", "IN_TRANSIT", "DELIVERED", "CANCELLED")) {
            move(id, status).andExpect(status().isConflict());
        }
    }

    @Test
    void aRefusedChangeRecordsNothing() throws Exception {
        long id = dispatchIdOf(placeOrder(1));
        Long before = jdbc.queryForObject(
                "SELECT COUNT(*) FROM delivery_tracking WHERE dispatch_id = ?", Long.class, id);

        move(id, "DELIVERED").andExpect(status().isConflict());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM delivery_tracking WHERE dispatch_id = ?", Long.class, id))
                .isEqualTo(before);
    }

    @Test
    void aShipmentCanOnlyBeCancelledThroughItsOrder() throws Exception {
        long id = dispatchIdOf(placeOrder(1));
        move(id, "CANCELLED")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A shipment is cancelled by cancelling its order."));
    }

    @Test
    void cancellingBeforeDispatchCancelsTheShipmentAndReleasesStock() throws Exception {
        long orderId = placeOrder(3);
        long id = dispatchIdOf(orderId);
        moveTo(id, "PACKED");

        mvc.perform(patch("/api/orders/" + orderId + "/cancel").cookie(customer)).andExpect(status().isOk());

        assertStock(10, 0);
        mvc.perform(get("/api/dispatches/" + id).cookie(admin))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.allowedNextStatuses.length()").value(0));
        move(id, "DISPATCHED").andExpect(status().isConflict());
    }

    @Test
    void anOrderCannotBeCancelledOnceItHasLeftTheWarehouse() throws Exception {
        long orderId = placeOrder(3);
        moveTo(dispatchIdOf(orderId), "PACKED", "DISPATCHED");

        mvc.perform(get("/api/orders/" + orderId).cookie(customer)).andExpect(jsonPath("$.cancellable").value(false));
        mvc.perform(patch("/api/orders/" + orderId + "/cancel").cookie(customer))
                .andExpect(status().isConflict());

        assertStock(7, 0);
    }

    // Tracking

    @Test
    void everyChangeIsRecordedWithWhoMadeIt() throws Exception {
        long orderId = placeOrder(1);
        long id = dispatchIdOf(orderId);
        moveTo(id, "PACKED", "DISPATCHED");

        mvc.perform(get("/api/tracking/dispatch/" + id).cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history.length()").value(3))
                .andExpect(jsonPath("$.history[0].fromStatus").isEmpty())
                .andExpect(jsonPath("$.history[0].toStatus").value("PENDING"))
                .andExpect(jsonPath("$.history[0].updatedBy").value("System"))
                .andExpect(jsonPath("$.history[1].fromStatus").value("PENDING"))
                .andExpect(jsonPath("$.history[1].toStatus").value("PACKED"))
                .andExpect(jsonPath("$.history[1].updatedBy").value("Test ADMIN"))
                .andExpect(jsonPath("$.history[1].note").value("Internal note"))
                .andExpect(jsonPath("$.history[2].toStatus").value("DISPATCHED"));
    }

    @Test
    void theTimelineOnlyShowsStepsThatHappened() throws Exception {
        long orderId = placeOrder(1);
        moveTo(dispatchIdOf(orderId), "PACKED");

        mvc.perform(get("/api/tracking/order/" + orderId).cookie(customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipmentStatus").value("PACKED"))
                .andExpect(jsonPath("$.warehouseName").value(warehouse.getName()))
                .andExpect(jsonPath("$.estimatedDeliveryDate").isNotEmpty())
                .andExpect(jsonPath("$.steps.length()").value(7))
                .andExpect(jsonPath("$.steps[0].label").value("Order placed"))
                .andExpect(jsonPath("$.steps[0].completedAt").isNotEmpty())
                .andExpect(jsonPath("$.steps[1].label").value("Order confirmed"))
                .andExpect(jsonPath("$.steps[1].completedAt").isNotEmpty())
                .andExpect(jsonPath("$.steps[2].label").value("Warehouse allocated"))
                .andExpect(jsonPath("$.steps[2].completedAt").isNotEmpty())
                .andExpect(jsonPath("$.steps[3].label").value("Packed"))
                .andExpect(jsonPath("$.steps[3].completedAt").isNotEmpty())
                .andExpect(jsonPath("$.steps[4].label").value("Dispatched"))
                .andExpect(jsonPath("$.steps[4].completedAt").isEmpty())
                .andExpect(jsonPath("$.steps[6].label").value("Delivered"))
                .andExpect(jsonPath("$.steps[6].completedAt").isEmpty());
    }

    @Test
    void aCancelledOrderShowsTheCancellationOnItsTimeline() throws Exception {
        long orderId = placeOrder(1);
        mvc.perform(patch("/api/orders/" + orderId + "/cancel").cookie(customer));

        mvc.perform(get("/api/tracking/order/" + orderId).cookie(customer))
                .andExpect(jsonPath("$.shipmentStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.steps[7].key").value("CANCELLED"))
                .andExpect(jsonPath("$.steps[7].completedAt").isNotEmpty());
    }

    @Test
    void customersDoNotSeeOperationalDetails() throws Exception {
        long orderId = placeOrder(1);
        moveTo(dispatchIdOf(orderId), "PACKED");

        mvc.perform(get("/api/tracking/order/" + orderId).cookie(customer))
                .andExpect(jsonPath("$.history[1].toStatus").value("PACKED"))
                .andExpect(jsonPath("$.history[1].updatedBy").isEmpty())
                .andExpect(jsonPath("$.history[1].note").isEmpty());
    }

    @Test
    void customersCannotTrackSomeoneElsesOrder() throws Exception {
        long orderId = placeOrder(1);

        mvc.perform(get("/api/tracking/order/" + orderId).cookie(sessions.customer()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/tracking/order/" + orderId)).andExpect(status().isUnauthorized());
    }

    // Access and lists

    @Test
    void dispatchManagementIsForAdministratorsOnly() throws Exception {
        long id = dispatchIdOf(placeOrder(1));

        mvc.perform(get("/api/dispatches").cookie(customer)).andExpect(status().isForbidden());
        mvc.perform(get("/api/dispatches/" + id).cookie(customer)).andExpect(status().isForbidden());
        mvc.perform(get("/api/tracking/dispatch/" + id).cookie(customer)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/dispatches/" + id + "/status")
                        .cookie(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PACKED\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/dispatches")).andExpect(status().isUnauthorized());
    }

    @Test
    void dispatchesCanBeSearchedAndFiltered() throws Exception {
        long orderId = placeOrder(1);
        Dispatch dispatch = dispatches.findByOrderId(orderId).orElseThrow();
        String warehouseId = warehouse.getId().toString();

        mvc.perform(get("/api/dispatches").cookie(admin).param("warehouseId", warehouseId))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].customerName").value("Test CUSTOMER"));
        mvc.perform(get("/api/dispatches").cookie(admin).param("q", dispatch.getTrackingReference()))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/dispatches")
                        .cookie(admin)
                        .param("warehouseId", warehouseId)
                        .param("status", "DELIVERED"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void aMissingDispatchIsNotFound() throws Exception {
        mvc.perform(get("/api/dispatches/999999999").cookie(admin)).andExpect(status().isNotFound());
        move(999_999_999L, "PACKED").andExpect(status().isNotFound());
        mvc.perform(get("/api/tracking/dispatch/999999999").cookie(admin)).andExpect(status().isNotFound());
    }

    @Test
    void warehousePerformanceReflectsRealShipments() throws Exception {
        long delivered = placeOrder(1);
        moveTo(dispatchIdOf(delivered), "PACKED", "DISPATCHED", "IN_TRANSIT", "DELIVERED");
        placeOrder(1);

        mvc.perform(get("/api/warehouses/" + warehouse.getId()).cookie(admin))
                .andExpect(jsonPath("$.performance.allocatedOrders").value(2))
                .andExpect(jsonPath("$.performance.activeDispatches").value(1))
                .andExpect(jsonPath("$.performance.completedDispatches").value(1))
                .andExpect(jsonPath("$.performance.fulfilmentRate").value(0.5));
    }
}
