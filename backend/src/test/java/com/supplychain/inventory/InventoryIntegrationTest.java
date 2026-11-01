package com.supplychain.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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
import com.supplychain.user.User;
import com.supplychain.user.UserRole;
import com.supplychain.warehouse.Warehouse;
import com.supplychain.warehouse.WarehouseStatus;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
@Import({TestSessions.class, TestCatalog.class})
class InventoryIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestSessions sessions;

    @Autowired
    private TestCatalog catalog;

    @Autowired
    private InventoryRepository inventory;

    @Autowired
    private InventoryTransactionRepository ledger;

    private ResultActions change(Cookie session, long id, String type, int quantity) throws Exception {
        return mvc.perform(put("/api/inventory/" + id)
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"%s\",\"quantity\":%d,\"reason\":\"Test reason\"}".formatted(type, quantity)));
    }

    private Inventory stocked(int available, int reserved, int reorderLevel) {
        return catalog.stock(catalog.product("Phone"), catalog.warehouse("Hubli"), available, reserved, reorderLevel);
    }

    private long ledgerCount(long inventoryId) {
        return ledger.search(inventoryId, null, null, null, Pageable.unpaged()).getTotalElements();
    }

    @Test
    void everyEndpointIsForAdministratorsOnly() throws Exception {
        long id = stocked(5, 0, 1).getId();

        mvc.perform(get("/api/inventory")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/inventory").cookie(sessions.customer())).andExpect(status().isForbidden());
        mvc.perform(get("/api/inventory/low-stock").cookie(sessions.customer())).andExpect(status().isForbidden());
        mvc.perform(get("/api/inventory/transactions").cookie(sessions.customer())).andExpect(status().isForbidden());
        change(sessions.customer(), id, "RESTOCK", 5).andExpect(status().isForbidden());
    }

    @Test
    void creatingARecordWritesOpeningStockToTheLedger() throws Exception {
        Product product = catalog.product("Phone");
        Warehouse warehouse = catalog.warehouse("Hubli");

        String response = mvc.perform(post("/api/inventory")
                        .cookie(sessions.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":%d,\"warehouseId\":%d,\"availableQuantity\":15,\"reorderLevel\":5}"
                                .formatted(product.getId(), warehouse.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.availableQuantity").value(15))
                .andExpect(jsonPath("$.reservedQuantity").value(0))
                .andExpect(jsonPath("$.lowStock").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        InventoryTransaction entry = ledger.search(null, warehouse.getId(), null, null, Pageable.unpaged())
                .getContent()
                .getFirst();
        assertThat(response).contains("\"id\":" + entry.getInventory().getId());
        assertThat(entry.getType()).isEqualTo(InventoryTransactionType.RESTOCK);
        assertThat(entry.getAvailableDelta()).isEqualTo(15);
        assertThat(entry.getAvailableAfter()).isEqualTo(15);
    }

    @Test
    void aSecondRecordForTheSamePairIsRefused() throws Exception {
        Inventory existing = stocked(5, 0, 1);

        mvc.perform(post("/api/inventory")
                        .cookie(sessions.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":%d,\"warehouseId\":%d,\"availableQuantity\":1,\"reorderLevel\":1}"
                                .formatted(existing.getProduct().getId(), existing.getWarehouse().getId())))
                .andExpect(status().isConflict());
    }

    @Test
    void restockAddsStockAndRecordsWhoAndWhy() throws Exception {
        Inventory record = stocked(10, 3, 5);
        User admin = sessions.user(UserRole.ADMIN);

        change(sessions.cookie(admin), record.getId(), "RESTOCK", 7)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(17))
                .andExpect(jsonPath("$.reservedQuantity").value(3));

        mvc.perform(get("/api/inventory/transactions")
                        .cookie(sessions.admin())
                        .param("inventoryId", record.getId().toString()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].type").value("RESTOCK"))
                .andExpect(jsonPath("$.content[0].availableDelta").value(7))
                .andExpect(jsonPath("$.content[0].reservedDelta").value(0))
                .andExpect(jsonPath("$.content[0].availableAfter").value(17))
                .andExpect(jsonPath("$.content[0].reservedAfter").value(3))
                .andExpect(jsonPath("$.content[0].note").value("Test reason"))
                .andExpect(jsonPath("$.content[0].performedBy").value(admin.getFullName()));
    }

    @Test
    void adjustmentMayGoEitherWayDownToZero() throws Exception {
        Inventory record = stocked(10, 0, 5);
        Cookie admin = sessions.admin();

        change(admin, record.getId(), "ADJUSTMENT", -10)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(0));
        change(admin, record.getId(), "ADJUSTMENT", 4)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(4));
        assertThat(ledgerCount(record.getId())).isEqualTo(2);
    }

    @Test
    void stockCannotGoNegativeAndAFailedChangeLeavesNoTrace() throws Exception {
        Inventory record = stocked(3, 0, 1);

        change(sessions.admin(), record.getId(), "ADJUSTMENT", -4)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("Only 3 available")));

        assertThat(inventory.findById(record.getId()).orElseThrow().getAvailableQuantity()).isEqualTo(3);
        assertThat(ledgerCount(record.getId())).isZero();
    }

    @Test
    void reservedStockIsNotAvailableForAdjustment() throws Exception {
        Inventory record = stocked(2, 8, 1);

        change(sessions.admin(), record.getId(), "ADJUSTMENT", -5).andExpect(status().isConflict());

        Inventory unchanged = inventory.findById(record.getId()).orElseThrow();
        assertThat(unchanged.getAvailableQuantity()).isEqualTo(2);
        assertThat(unchanged.getReservedQuantity()).isEqualTo(8);
    }

    @Test
    void restockCannotRemoveStock() throws Exception {
        change(sessions.admin(), stocked(5, 0, 1).getId(), "RESTOCK", -1).andExpect(status().isBadRequest());
    }

    @Test
    void aZeroChangeWithNothingElseIsRefused() throws Exception {
        change(sessions.admin(), stocked(5, 0, 1).getId(), "ADJUSTMENT", 0).andExpect(status().isBadRequest());
    }

    @Test
    void aReasonIsRequired() throws Exception {
        mvc.perform(put("/api/inventory/" + stocked(5, 0, 1).getId())
                        .cookie(sessions.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"RESTOCK\",\"quantity\":1,\"reason\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("reason"));
    }

    @Test
    void changingOnlyTheReorderLevelTouchesNoStockAndNoLedger() throws Exception {
        Inventory record = stocked(5, 0, 1);

        mvc.perform(put("/api/inventory/" + record.getId())
                        .cookie(sessions.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ADJUSTMENT\",\"quantity\":0,\"reorderLevel\":9,\"reason\":\"Policy\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(5))
                .andExpect(jsonPath("$.reorderLevel").value(9))
                .andExpect(jsonPath("$.lowStock").value(true));

        assertThat(ledgerCount(record.getId())).isZero();
    }

    @Test
    void aMissingRecordIsNotFound() throws Exception {
        change(sessions.admin(), 999_999_999L, "RESTOCK", 1).andExpect(status().isNotFound());
    }

    @Test
    void lowStockListsOnlyItemsBelowReorderOnSaleInOperatingWarehouses() throws Exception {
        Warehouse active = catalog.warehouse("Active");
        Warehouse inactive = catalog.warehouse("Inactive", WarehouseStatus.INACTIVE);
        Product low = catalog.product("LowStockTarget");
        catalog.stock(low, active, 2, 0, 5);
        catalog.stock(catalog.product("AtLevel"), active, 5, 0, 5);
        catalog.stock(catalog.product("InactiveWarehouseItem"), inactive, 2, 0, 5);
        catalog.stock(
                catalog.product("InactiveProductItem", catalog.category(), ProductStatus.INACTIVE), active, 2, 0, 5);

        mvc.perform(get("/api/inventory")
                        .cookie(sessions.admin())
                        .param("lowStockOnly", "true")
                        .param("warehouseId", active.getId().toString()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].productName").value("LowStockTarget"));

        mvc.perform(get("/api/inventory/low-stock").cookie(sessions.admin()).param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.productName == 'LowStockTarget')]").exists())
                .andExpect(jsonPath("$.content[?(@.productName == 'InactiveWarehouseItem')]").doesNotExist())
                .andExpect(jsonPath("$.content[?(@.productName == 'InactiveProductItem')]").doesNotExist())
                .andExpect(jsonPath("$.content[?(@.productName == 'AtLevel')]").doesNotExist());
    }

    @Test
    void listsFilterByWarehouseAndProduct() throws Exception {
        Warehouse hubli = catalog.warehouse("Hubli");
        Warehouse mysuru = catalog.warehouse("Mysuru");
        Product phone = catalog.product("Filter Phone");
        catalog.stock(phone, hubli, 1, 0, 0);
        catalog.stock(phone, mysuru, 2, 0, 0);
        catalog.stock(catalog.product("Filter Laptop"), hubli, 3, 0, 0);

        mvc.perform(get("/api/inventory/warehouse/" + hubli.getId()).cookie(sessions.admin()))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/inventory")
                        .cookie(sessions.admin())
                        .param("warehouseId", hubli.getId().toString())
                        .param("q", "phone"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].availableQuantity").value(1));
    }

    @Test
    void historyListsNewestFirstAndFiltersByType() throws Exception {
        Inventory record = stocked(10, 0, 1);
        Cookie admin = sessions.admin();
        change(admin, record.getId(), "RESTOCK", 1);
        change(admin, record.getId(), "ADJUSTMENT", -2);
        String inventoryId = record.getId().toString();

        mvc.perform(get("/api/inventory/transactions").cookie(admin).param("inventoryId", inventoryId))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].type").value("ADJUSTMENT"))
                .andExpect(jsonPath("$.content[1].type").value("RESTOCK"));

        mvc.perform(get("/api/inventory/transactions")
                        .cookie(admin)
                        .param("inventoryId", inventoryId)
                        .param("type", "RESTOCK"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
