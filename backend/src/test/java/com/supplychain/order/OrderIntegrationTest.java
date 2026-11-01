package com.supplychain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.supplychain.IntegrationTest;
import com.supplychain.TestCatalog;
import com.supplychain.TestSessions;
import com.supplychain.product.Product;
import com.supplychain.product.ProductRepository;
import com.supplychain.product.ProductStatus;
import com.supplychain.user.User;
import com.supplychain.user.UserRole;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
@Import({TestSessions.class, TestCatalog.class})
class OrderIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestSessions sessions;

    @Autowired
    private TestCatalog catalog;

    @Autowired
    private ProductRepository products;

    @Autowired
    private JdbcTemplate jdbc;

    private static String body(String items) {
        return """
                {"items":%s,"deliveryName":"Asha Rao","deliveryPhone":"9876543210","deliveryAddress":"12 MG Road",
                 "deliveryCity":"Davanagere","deliveryPostalCode":"577001","priority":"STANDARD",
                 "simulatedPaymentConfirmed":true}
                """.formatted(items);
    }

    private static String oneLine(Product product, int quantity) {
        return body("[{\"productId\":%d,\"quantity\":%d}]".formatted(product.getId(), quantity));
    }

    private ResultActions place(Cookie session, String key, String json) throws Exception {
        return mvc.perform(post("/api/orders")
                .cookie(session)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions place(Cookie session, String json) throws Exception {
        return place(session, UUID.randomUUID().toString(), json);
    }

    private long idOf(ResultActions result) throws Exception {
        String response = result.andReturn().getResponse().getContentAsString();
        return Long.parseLong(response.replaceAll("^\\{\"id\":(\\d+),.*$", "$1"));
    }

    @Test
    void anOrderIsCreatedWithServerPricesAndSnapshottedDelivery() throws Exception {
        Product phone = catalog.product("Phone");
        Product laptop = catalog.product("Laptop");

        place(
                        sessions.customer(),
                        body("[{\"productId\":%d,\"quantity\":2},{\"productId\":%d,\"quantity\":1}]"
                                .formatted(phone.getId(), laptop.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber", matchesPattern("^ORD-\\d{4}-\\d{6}$")))
                .andExpect(jsonPath("$.totalAmount").value(2998.50))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].productName").value("Phone"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(999.50))
                .andExpect(jsonPath("$.items[0].subtotal").value(1999.00))
                .andExpect(jsonPath("$.deliveryCity").value("Davanagere"))
                .andExpect(jsonPath("$.deliveryState").value("Karnataka"))
                .andExpect(jsonPath("$.priority").value("STANDARD"));
    }

    @Test
    void anOrderWithoutStockStaysUnconfirmed() throws Exception {
        place(sessions.customer(), oneLine(catalog.product("Phone"), 2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentStatus").value("UNPAID"))
                .andExpect(jsonPath("$.selectedWarehouse").value(nullValue()));
    }

    @Test
    void priceAndTotalSentByTheClientAreIgnored() throws Exception {
        Product phone = catalog.product("Phone");
        String tampered = body(
                "[{\"productId\":%d,\"quantity\":1,\"unitPrice\":1}]".formatted(phone.getId()))
                .replace("\"priority\"", "\"totalAmount\":1,\"priority\"");

        place(sessions.customer(), tampered)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(999.50));
    }

    @Test
    void aLaterPriceChangeDoesNotAlterAnExistingOrder() throws Exception {
        Product phone = catalog.product("Phone");
        Cookie session = sessions.customer();
        long id = idOf(place(session, oneLine(phone, 1)));

        phone.setPrice(new BigDecimal("5.00"));
        products.saveAndFlush(phone);

        mvc.perform(get("/api/orders/" + id).cookie(session))
                .andExpect(jsonPath("$.totalAmount").value(999.50))
                .andExpect(jsonPath("$.items[0].unitPrice").value(999.50));
    }

    @Test
    void repeatedLinesForAProductAreMerged() throws Exception {
        Product phone = catalog.product("Phone");
        String items = "[{\"productId\":%1$d,\"quantity\":2},{\"productId\":%1$d,\"quantity\":3}]"
                .formatted(phone.getId());

        place(sessions.customer(), body(items))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(5))
                .andExpect(jsonPath("$.totalAmount").value(4997.50));
    }

    @Test
    void theSameIdempotencyKeyReturnsTheOriginalOrder() throws Exception {
        Product phone = catalog.product("Phone");
        Cookie session = sessions.customer();

        long first = idOf(place(session, "double-click-key", oneLine(phone, 1)).andExpect(status().isCreated()));
        long second = idOf(place(session, "double-click-key", oneLine(phone, 9)).andExpect(status().isCreated()));

        assertThat(second).isEqualTo(first);
        mvc.perform(get("/api/orders/" + first).cookie(session)).andExpect(jsonPath("$.items[0].quantity").value(1));
    }

    @Test
    void anotherCustomerMayUseTheSameIdempotencyKey() throws Exception {
        Product phone = catalog.product("Phone");

        long first = idOf(place(sessions.customer(), "shared-key-value", oneLine(phone, 1)));
        long second = idOf(place(sessions.customer(), "shared-key-value", oneLine(phone, 1)));

        assertThat(second).isNotEqualTo(first);
    }

    @Test
    void anIdempotencyKeyIsRequired() throws Exception {
        mvc.perform(post("/api/orders")
                        .cookie(sessions.customer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oneLine(catalog.product("Phone"), 1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anIdempotencyKeyOfTheWrongLengthIsRefused() throws Exception {
        String json = oneLine(catalog.product("Phone"), 1);
        place(sessions.customer(), "short", json).andExpect(status().isBadRequest());
        place(sessions.customer(), "x".repeat(65), json).andExpect(status().isBadRequest());
    }

    @Test
    void anInactiveOrUnknownProductIsRefusedAndNothingIsSaved() throws Exception {
        Product inactive = catalog.product("Old phone", catalog.category(), ProductStatus.INACTIVE);
        Long before = jdbc.queryForObject("SELECT COUNT(*) FROM orders", Long.class);

        place(sessions.customer(), oneLine(inactive, 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Old phone in your cart is no longer available. Remove it and try again."));
        place(sessions.customer(), body("[{\"productId\":999999999,\"quantity\":1}]"))
                .andExpect(status().isConflict());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders", Long.class)).isEqualTo(before);
    }

    @Test
    void aCityOutsideTheListIsRefused() throws Exception {
        String json = oneLine(catalog.product("Phone"), 1).replace("Davanagere", "Atlantis");
        place(sessions.customer(), json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("We don't deliver to this city yet."));
    }

    @Test
    void theSimulatedPaymentMustBeConfirmed() throws Exception {
        String json = oneLine(catalog.product("Phone"), 1)
                .replace("\"simulatedPaymentConfirmed\":true", "\"simulatedPaymentConfirmed\":false");
        place(sessions.customer(), json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("simulatedPaymentConfirmed"));
    }

    @Test
    void invalidOrdersReportEachField() throws Exception {
        place(sessions.customer(), """
                {"items":[],"deliveryName":"","deliveryPhone":"123","deliveryAddress":"","deliveryCity":"",
                 "deliveryPostalCode":"1","simulatedPaymentConfirmed":true}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("items")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("deliveryName")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("deliveryPhone")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("deliveryCity")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("deliveryPostalCode")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("priority")));
    }

    @Test
    void lineQuantitiesAreLimited() throws Exception {
        String items = "[{\"productId\":1,\"quantity\":%d}]";
        place(sessions.customer(), body(items.formatted(0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("items[0].quantity"));
        place(sessions.customer(), body(items.formatted(100))).andExpect(status().isBadRequest());
    }

    @Test
    void onlyCustomersCanPlaceOrders() throws Exception {
        String json = oneLine(catalog.product("Phone"), 1);

        mvc.perform(post("/api/orders")
                        .header("Idempotency-Key", "anonymous-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
        place(sessions.admin(), json).andExpect(status().isForbidden());
    }

    @Test
    void customersSeeOnlyTheirOwnOrders() throws Exception {
        Product phone = catalog.product("Phone");
        Cookie owner = sessions.customer();
        Cookie other = sessions.customer();
        long id = idOf(place(owner, oneLine(phone, 1)));

        mvc.perform(get("/api/orders/" + id).cookie(owner)).andExpect(status().isOk());
        // The same response as for an order that doesn't exist.
        mvc.perform(get("/api/orders/" + id).cookie(other)).andExpect(status().isNotFound());
        mvc.perform(get("/api/orders/999999999").cookie(other)).andExpect(status().isNotFound());
        mvc.perform(get("/api/orders/" + id)).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/orders/my-orders").cookie(owner))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id))
                .andExpect(jsonPath("$.content[0].unitCount").value(1));
        mvc.perform(get("/api/orders/my-orders").cookie(other)).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void administratorsSeeEveryOrderAndCanFilter() throws Exception {
        Product phone = catalog.product("Phone");
        User customer = sessions.user(UserRole.CUSTOMER);
        Cookie session = sessions.cookie(customer);
        long id = idOf(place(session, oneLine(phone, 1)));
        Cookie admin = sessions.admin();

        mvc.perform(get("/api/orders/" + id).cookie(admin)).andExpect(status().isOk());
        mvc.perform(get("/api/orders").cookie(admin).param("q", customer.getEmail()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].customerName").value(customer.getFullName()));
        mvc.perform(get("/api/orders").cookie(admin).param("q", customer.getEmail()).param("status", "DELIVERED"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void onlyAdministratorsCanListEveryOrder() throws Exception {
        mvc.perform(get("/api/orders").cookie(sessions.customer())).andExpect(status().isForbidden());
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void administratorsDoNotHaveAPersonalOrderList() throws Exception {
        mvc.perform(get("/api/orders/my-orders").cookie(sessions.admin())).andExpect(status().isForbidden());
    }

    @Test
    void theCityListIsPublic() throws Exception {
        mvc.perform(get("/api/locations/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(16))
                .andExpect(jsonPath("$[*].name", hasItem("Davanagere")))
                .andExpect(jsonPath("$[0].state").value("Karnataka"));
    }
}
