package com.supplychain.warehouse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.supplychain.IntegrationTest;
import com.supplychain.TestCatalog;
import com.supplychain.TestSessions;
import com.supplychain.product.Product;
import com.supplychain.product.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@Import({TestSessions.class, TestCatalog.class})
class WarehouseIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestSessions sessions;

    @Autowired
    private TestCatalog catalog;

    @Autowired
    private WarehouseRepository warehouses;

    private static String body(String name) {
        return """
                {"name":"%s","address":"Gokul Road","city":"Hubli","state":"Karnataka","postalCode":"580030",
                 "latitude":15.3647,"longitude":75.124,"contactNumber":"+91 9876543210",
                 "baseDeliveryCost":120,"costPerKm":14,"handlingHours":12}
                """.formatted(name);
    }

    @Test
    void everyEndpointIsForAdministratorsOnly() throws Exception {
        long id = catalog.warehouse("Hubli").getId();

        mvc.perform(get("/api/warehouses")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/warehouses").cookie(sessions.customer())).andExpect(status().isForbidden());
        mvc.perform(get("/api/warehouses/" + id).cookie(sessions.customer())).andExpect(status().isForbidden());
        mvc.perform(post("/api/warehouses")
                        .cookie(sessions.customer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("X")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/warehouses/" + id).cookie(sessions.customer())).andExpect(status().isForbidden());
    }

    @Test
    void administratorCreatesAndEdits() throws Exception {
        String name = TestCatalog.unique("Hubli");
        mvc.perform(post("/api/warehouses")
                        .cookie(sessions.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(name)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.stock.productCount").value(0));

        long id = catalog.warehouse("Mysuru").getId();
        mvc.perform(put("/api/warehouses/" + id)
                        .cookie(sessions.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(name + "-renamed")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name + "-renamed"))
                .andExpect(jsonPath("$.handlingHours").value(12));
    }

    @Test
    void namesMustBeUnique() throws Exception {
        Warehouse existing = catalog.warehouse("Hubli");

        mvc.perform(post("/api/warehouses")
                        .cookie(sessions.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(existing.getName())))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidWarehousesReportEachField() throws Exception {
        mvc.perform(post("/api/warehouses")
                        .cookie(sessions.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","postalCode":"12","latitude":91,"longitude":-181,"baseDeliveryCost":-1,"handlingHours":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("postalCode")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("latitude")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("longitude")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("baseDeliveryCost")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("handlingHours")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("costPerKm")));
    }

    @Test
    void deleteOnlyDeactivatesAndAnEditCanReactivate() throws Exception {
        Warehouse warehouse = catalog.warehouse("Hubli");

        mvc.perform(delete("/api/warehouses/" + warehouse.getId()).cookie(sessions.admin()))
                .andExpect(status().isNoContent());
        assertThat(warehouses.findById(warehouse.getId()).orElseThrow().getStatus())
                .isEqualTo(WarehouseStatus.INACTIVE);

        mvc.perform(put("/api/warehouses/" + warehouse.getId())
                        .cookie(sessions.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(warehouse.getName()).replace("\"handlingHours\":12", "\"handlingHours\":12,\"status\":\"ACTIVE\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void listAndDetailSummariseStock() throws Exception {
        Warehouse warehouse = catalog.warehouse("Summary");
        catalog.stock(catalog.product("A"), warehouse, 10, 2, 5);
        catalog.stock(catalog.product("B"), warehouse, 1, 0, 5);
        Product inactive = catalog.product("C", catalog.category(), ProductStatus.INACTIVE);
        catalog.stock(inactive, warehouse, 0, 0, 5);

        mvc.perform(get("/api/warehouses").cookie(sessions.admin()).param("q", warehouse.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].stock.productCount").value(3))
                .andExpect(jsonPath("$.content[0].stock.totalAvailable").value(11))
                .andExpect(jsonPath("$.content[0].stock.totalReserved").value(2))
                // An inactive product doesn't need reordering.
                .andExpect(jsonPath("$.content[0].stock.lowStockItems").value(1));

        mvc.perform(get("/api/warehouses/" + warehouse.getId()).cookie(sessions.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warehouse.stock.totalAvailable").value(11))
                .andExpect(jsonPath("$.performance.allocatedOrders").value(0))
                .andExpect(jsonPath("$.performance.fulfilmentRate").value(nullValue()));
    }

    @Test
    void aMissingWarehouseIsNotFound() throws Exception {
        mvc.perform(get("/api/warehouses/999999999").cookie(sessions.admin())).andExpect(status().isNotFound());
    }
}
