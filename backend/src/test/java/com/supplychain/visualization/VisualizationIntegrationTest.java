package com.supplychain.visualization;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.supplychain.IntegrationTest;
import com.supplychain.TestCatalog;
import com.supplychain.TestSessions;
import com.supplychain.dispatch.DispatchRepository;
import com.supplychain.product.Product;
import com.supplychain.warehouse.Warehouse;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@Import({TestSessions.class, TestCatalog.class})
class VisualizationIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestSessions sessions;

    @Autowired
    private TestCatalog catalog;

    @Autowired
    private DispatchRepository dispatches;

    private long placeOrder(Cookie customer, Product product, int quantity) throws Exception {
        String response = mvc.perform(post("/api/orders")
                        .cookie(customer)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"productId":%d,"quantity":%d}],"deliveryName":"Asha Rao",
                                 "deliveryPhone":"9876543210","deliveryAddress":"12 MG Road","deliveryCity":"Davanagere",
                                 "deliveryPostalCode":"577001","priority":"STANDARD","simulatedPaymentConfirmed":true}
                                """.formatted(product.getId(), quantity)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return Long.parseLong(response.replaceAll("^\\{\"id\":(\\d+),.*$", "$1"));
    }

    @Test
    void theNetworkIsBuiltFromRealRecords() throws Exception {
        Warehouse warehouse = catalog.warehouse("Hubli");
        Product phone = catalog.product("Phone");
        catalog.stock(phone, warehouse, 10, 0, 1);
        catalog.stock(catalog.product("Empty"), warehouse, 0, 0, 5);
        long orderId = placeOrder(sessions.customer(), phone, 3);

        mvc.perform(get("/api/visualization/network").cookie(sessions.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warehouses", hasSize(1)))
                .andExpect(jsonPath("$.warehouses[0].name").value(warehouse.getName()))
                .andExpect(jsonPath("$.warehouses[0].latitude").value(15.3647))
                .andExpect(jsonPath("$.warehouses[0].productCount").value(2))
                .andExpect(jsonPath("$.warehouses[0].inStockProducts").value(1))
                .andExpect(jsonPath("$.warehouses[0].totalAvailable").value(7))
                .andExpect(jsonPath("$.warehouses[0].totalReserved").value(3))
                .andExpect(jsonPath("$.warehouses[0].lowStockItems").value(1))
                .andExpect(jsonPath("$.warehouses[0].activeOrders").value(1))
                .andExpect(jsonPath("$.warehouses[0].activeDispatches").value(1))
                .andExpect(jsonPath("$.shipments", hasSize(1)))
                .andExpect(jsonPath("$.shipments[0].orderId").value(orderId))
                .andExpect(jsonPath("$.shipments[0].warehouseId").value(warehouse.getId()))
                .andExpect(jsonPath("$.shipments[0].destinationCity").value("Davanagere"))
                .andExpect(jsonPath("$.shipments[0].latitude").value(14.4644))
                .andExpect(jsonPath("$.shipments[0].status").value("PENDING"));
    }

    @Test
    void anEmptyNetworkIsValid() throws Exception {
        mvc.perform(get("/api/visualization/network").cookie(sessions.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warehouses", hasSize(0)))
                .andExpect(jsonPath("$.shipments", hasSize(0)));
    }

    @Test
    void theMapFollowsShipmentStatus() throws Exception {
        Warehouse warehouse = catalog.warehouse("Hubli");
        Product phone = catalog.product("Phone");
        catalog.stock(phone, warehouse, 10, 0, 1);
        long orderId = placeOrder(sessions.customer(), phone, 1);
        Cookie admin = sessions.admin();
        long dispatchId = dispatches.findByOrderId(orderId).orElseThrow().getId();

        for (String next : new String[] {"PACKED", "DISPATCHED", "IN_TRANSIT", "DELIVERED"}) {
            mvc.perform(patch("/api/dispatches/" + dispatchId + "/status")
                            .cookie(admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"%s\"}".formatted(next)))
                    .andExpect(status().isOk());
            mvc.perform(get("/api/visualization/network").cookie(admin))
                    .andExpect(jsonPath("$.shipments[0].status").value(next));
        }

        mvc.perform(get("/api/visualization/network").cookie(admin))
                .andExpect(jsonPath("$.warehouses[0].activeOrders").value(0))
                .andExpect(jsonPath("$.warehouses[0].activeDispatches").value(0));
    }

    @Test
    void cancelledShipmentsLeaveTheMapAndStopCounting() throws Exception {
        Warehouse warehouse = catalog.warehouse("Hubli");
        Product phone = catalog.product("Phone");
        catalog.stock(phone, warehouse, 10, 0, 1);
        Cookie customer = sessions.customer();
        long orderId = placeOrder(customer, phone, 1);

        mvc.perform(patch("/api/orders/" + orderId + "/cancel").cookie(customer)).andExpect(status().isOk());

        mvc.perform(get("/api/visualization/network").cookie(sessions.admin()))
                .andExpect(jsonPath("$.shipments", hasSize(0)))
                .andExpect(jsonPath("$.warehouses[0].activeOrders").value(0))
                .andExpect(jsonPath("$.warehouses[0].activeDispatches").value(0));
    }

    @Test
    void anAllocationIncludesTheDeliveryLocationForReplay() throws Exception {
        Warehouse warehouse = catalog.warehouse("Hubli");
        Product phone = catalog.product("Phone");
        catalog.stock(phone, warehouse, 10, 0, 1);
        long orderId = placeOrder(sessions.customer(), phone, 1);

        mvc.perform(get("/api/allocations/" + orderId).cookie(sessions.admin()))
                .andExpect(jsonPath("$.deliveryCity").value("Davanagere"))
                .andExpect(jsonPath("$.deliveryLatitude").value(14.4644))
                .andExpect(jsonPath("$.deliveryLongitude").value(75.9218));
    }

    @Test
    void theNetworkIsForAdministratorsOnly() throws Exception {
        mvc.perform(get("/api/visualization/network").cookie(sessions.customer())).andExpect(status().isForbidden());
        mvc.perform(get("/api/visualization/network")).andExpect(status().isUnauthorized());
    }
}
