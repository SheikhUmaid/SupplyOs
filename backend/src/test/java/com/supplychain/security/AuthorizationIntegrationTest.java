package com.supplychain.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.supplychain.IntegrationTest;
import com.supplychain.user.User;
import com.supplychain.user.UserRepository;
import com.supplychain.user.UserRole;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@IntegrationTest
@Import(AuthorizationIntegrationTest.AdminOnlyTestController.class)
class AuthorizationIntegrationTest {

    /** Exists only in tests. Phase 3 replaces it with real administrator endpoints. */
    @RestController
    static class AdminOnlyTestController {
        @GetMapping("/api/test/admin-only")
        @PreAuthorize("hasRole('ADMIN')")
        String adminOnly() {
            return "secret";
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private JwtService jwtService;

    private User user(UserRole role) {
        return users.saveAndFlush(new User(
                "Test User", role + "@example.com", "unused-hash", null, role, Instant.now()));
    }

    private Cookie sessionFor(User user) {
        return new Cookie(AuthCookie.NAME, jwtService.issue(user.getId()));
    }

    @Test
    void anonymousRequestsGetA401ProblemResponse() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Please log in to continue."));
    }

    @Test
    void unlistedRoutesAreDeniedByDefault() throws Exception {
        mvc.perform(get("/api/anything-not-listed")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/products")).andExpect(status().isUnauthorized());
    }

    @Test
    void customersGetA403FromAdminEndpoints() throws Exception {
        mvc.perform(get("/api/test/admin-only").cookie(sessionFor(user(UserRole.CUSTOMER))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void anonymousRequestsGetA401FromAdminEndpoints() throws Exception {
        mvc.perform(get("/api/test/admin-only")).andExpect(status().isUnauthorized());
    }

    @Test
    void administratorsReachAdminEndpoints() throws Exception {
        mvc.perform(get("/api/test/admin-only").cookie(sessionFor(user(UserRole.ADMIN))))
                .andExpect(status().isOk());
    }

    @Test
    void aForgedCookieIsTreatedAsAnonymous() throws Exception {
        mvc.perform(get("/api/auth/me").cookie(new Cookie(AuthCookie.NAME, "forged.token.value")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disablingAnAccountEndsItsExistingSession() throws Exception {
        User user = user(UserRole.CUSTOMER);
        Cookie session = sessionFor(user);
        mvc.perform(get("/api/auth/me").cookie(session)).andExpect(status().isOk());

        user.setEnabled(false);
        users.saveAndFlush(user);

        mvc.perform(get("/api/auth/me").cookie(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenForADeletedUserIsRejected() throws Exception {
        mvc.perform(get("/api/auth/me").cookie(new Cookie(AuthCookie.NAME, jwtService.issue(999_999_999L))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerDocumentsTheApi() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists());
    }
}
