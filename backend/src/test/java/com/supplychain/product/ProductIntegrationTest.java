package com.supplychain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.supplychain.IntegrationTest;
import com.supplychain.TestCatalog;
import com.supplychain.TestSessions;
import com.supplychain.category.Category;
import com.supplychain.warehouse.Warehouse;
import com.supplychain.warehouse.WarehouseStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@Import({TestSessions.class, TestCatalog.class})
class ProductIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestSessions sessions;

    @Autowired
    private TestCatalog catalog;

    @Autowired
    private ProductRepository products;

    private String body(String sku, long categoryId) {
        return """
                {"name":"Laptop","sku":"%s","description":"Thin","categoryId":%d,"price":54990.50,"imageUrl":"/products/laptop.svg"}
                """.formatted(sku, categoryId);
    }

    @Test
    void administratorCreatesAProductThatIsActiveByDefault() throws Exception {
        Category category = catalog.category();

        mvc.perform(post("/api/products")
                        .cookie(sessions.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("LAP-1", category.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("LAP-1"))
                .andExpect(jsonPath("$.price").value(54990.50))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.categoryName").value(category.getName()))
                .andExpect(jsonPath("$.availability").value("OUT"));
    }

    @Test
    void writesRequireAnAdministrator() throws Exception {
        long categoryId = catalog.category().getId();
        long productId = catalog.product("Phone").getId();
        String payload = body("LAP-1", categoryId);

        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/products")
                        .cookie(sessions.customer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/products/" + productId)
                        .cookie(sessions.customer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/products/" + productId).cookie(sessions.customer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void skuMustBeUniqueIgnoringCaseOnCreateAndUpdate() throws Exception {
        Product existing = catalog.product("Phone");
        Product other = catalog.product("Tablet");
        long categoryId = catalog.category().getId();

        mvc.perform(post("/api/products")
                        .cookie(sessions.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(existing.getSku().toLowerCase(), categoryId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A product with this SKU already exists."));

        mvc.perform(put("/api/products/" + other.getId())
                        .cookie(sessions.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(existing.getSku(), categoryId)))
                .andExpect(status().isConflict());
    }

    @Test
    void aProductMayKeepItsOwnSkuWhenEdited() throws Exception {
        Product product = catalog.product("Phone");

        mvc.perform(put("/api/products/" + product.getId())
                        .cookie(sessions.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(product.getSku(), product.getCategory().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void invalidProductsReportEachField() throws Exception {
        mvc.perform(post("/api/products")
                        .cookie(sessions.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","sku":"has spaces","price":-1,"imageUrl":"javascript:alert(1)"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("sku")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("price")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("categoryId")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("imageUrl")));
    }

    @Test
    void anUnknownCategoryIsRefused() throws Exception {
        mvc.perform(post("/api/products")
                        .cookie(sessions.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("LAP-1", 999_999_999L)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteOnlyDeactivates() throws Exception {
        Product product = catalog.product("Phone");

        mvc.perform(delete("/api/products/" + product.getId()).cookie(sessions.admin()))
                .andExpect(status().isNoContent());

        assertThat(products.findById(product.getId()).orElseThrow().getStatus()).isEqualTo(ProductStatus.INACTIVE);
    }

    @Test
    void inactiveProductsAreHiddenFromEveryoneButAdministrators() throws Exception {
        Category category = catalog.category();
        Product hidden = catalog.product("Hidden phone", category, ProductStatus.INACTIVE);
        String categoryId = category.getId().toString();

        mvc.perform(get("/api/products/" + hidden.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/products/" + hidden.getId()).cookie(sessions.customer()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/products/" + hidden.getId()).cookie(sessions.admin()))
                .andExpect(status().isOk());

        mvc.perform(get("/api/products").param("categoryId", categoryId))
                .andExpect(jsonPath("$.totalElements").value(0));
        // Asking for inactive products directly doesn't work around it.
        mvc.perform(get("/api/products").param("categoryId", categoryId).param("status", "INACTIVE"))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/products").param("categoryId", categoryId).cookie(sessions.admin()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void searchMatchesNameOrSkuAndTreatsWildcardsAsText() throws Exception {
        Category category = catalog.category();
        catalog.product("100% Cotton Shirt", category, ProductStatus.ACTIVE);
        catalog.product("1000 Piece Puzzle", category, ProductStatus.ACTIVE);
        Product sku = catalog.product("Other", category, ProductStatus.ACTIVE);
        String categoryId = category.getId().toString();

        mvc.perform(get("/api/products").param("categoryId", categoryId).param("q", "100%"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("100% Cotton Shirt"));
        mvc.perform(get("/api/products").param("categoryId", categoryId).param("q", "COTTON"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/products").param("categoryId", categoryId).param("q", sku.getSku()))
                .andExpect(jsonPath("$.content[0].name").value("Other"));
    }

    @Test
    void listsArePagedAndSorted() throws Exception {
        Category category = catalog.category();
        catalog.product("Alpha", category, ProductStatus.ACTIVE);
        catalog.product("Bravo", category, ProductStatus.ACTIVE);
        catalog.product("Charlie", category, ProductStatus.ACTIVE);

        mvc.perform(get("/api/products")
                        .param("categoryId", category.getId().toString())
                        .param("size", "2")
                        .param("page", "1")
                        .param("sort", "name,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Alpha"));
    }

    @Test
    void sortingOnAnUnlistedFieldIsABadRequest() throws Exception {
        mvc.perform(get("/api/products").param("sort", "category.secret,asc")).andExpect(status().isBadRequest());
    }

    @Test
    void availabilityReflectsStockInActiveWarehousesOnly() throws Exception {
        Warehouse active = catalog.warehouse("Active");
        Warehouse inactive = catalog.warehouse("Inactive", WarehouseStatus.INACTIVE);
        Product inStock = catalog.product("In stock");
        catalog.stock(inStock, active, 20, 0, 5);
        Product low = catalog.product("Low");
        catalog.stock(low, active, 3, 0, 5);
        Product out = catalog.product("Out");
        catalog.stock(out, active, 0, 4, 5);
        Product onlyInactive = catalog.product("Only inactive");
        catalog.stock(onlyInactive, inactive, 50, 0, 5);

        assertAvailability(inStock, "IN_STOCK");
        assertAvailability(low, "LOW");
        assertAvailability(out, "OUT");
        assertAvailability(onlyInactive, "OUT");
    }

    private void assertAvailability(Product product, String expected) throws Exception {
        mvc.perform(get("/api/products/" + product.getId()))
                .andExpect(jsonPath("$.availability").value(expected));
    }

    @Test
    void publicResponsesNeverExposeStockCountsOrWarehouses() throws Exception {
        Warehouse warehouse = catalog.warehouse("Secret location");
        Product product = catalog.product("Phone");
        catalog.stock(product, warehouse, 1234, 0, 5);

        mvc.perform(get("/api/products/" + product.getId()))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("1234"))))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("Secret location"))));
    }
}
