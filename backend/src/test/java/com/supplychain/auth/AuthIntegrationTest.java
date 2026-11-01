package com.supplychain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.supplychain.IntegrationTest;
import com.supplychain.security.AuthCookie;
import com.supplychain.user.User;
import com.supplychain.user.UserRepository;
import com.supplychain.user.UserRole;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@IntegrationTest
class AuthIntegrationTest {

    private static final String VALID_REGISTRATION = """
            {"fullName":"Asha Rao","email":"asha@example.com","password":"correct-horse","phone":"9876543210"}
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MvcResult postJson(String path, String body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
    }

    private static String login(String email, String password) {
        return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
    }

    @Test
    void registrationCreatesACustomerAndStartsASession() throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REGISTRATION))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("asha@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andReturn();

        assertThat(result.getResponse().getCookie(AuthCookie.NAME)).isNotNull();
    }

    @Test
    void registrationCannotCreateAnAdministrator() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Mallory","email":"m@example.com","password":"correct-horse","role":"ADMIN"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        assertThat(users.findByEmail("m@example.com").orElseThrow().getRole()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    void passwordsAreStoredHashedAndNeverReturned() throws Exception {
        MvcResult result = postJson("/api/auth/register", VALID_REGISTRATION);

        String hash = users.findByEmail("asha@example.com").orElseThrow().getPasswordHash();
        assertThat(hash).startsWith("$2").isNotEqualTo("correct-horse");
        assertThat(passwordEncoder.matches("correct-horse", hash)).isTrue();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("password", "$2");
    }

    @Test
    void sessionCookieIsHttpOnlyAndSameSite() throws Exception {
        String header = postJson("/api/auth/register", VALID_REGISTRATION)
                .getResponse()
                .getHeader(HttpHeaders.SET_COOKIE);

        assertThat(header).contains("HttpOnly", "SameSite=Lax", "Secure", "Path=/", "Max-Age=28800");
    }

    @Test
    void duplicateEmailIsRefusedRegardlessOfCase() throws Exception {
        postJson("/api/auth/register", VALID_REGISTRATION);

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REGISTRATION.replace("asha@example.com", "ASHA@Example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("An account with this email already exists."));
    }

    @Test
    void invalidRegistrationReportsEachField() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"","email":"nope","password":"short","phone":"123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("fullName")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("email")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("password")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("phone")));
    }

    @Test
    void malformedJsonIsABadRequestRatherThanAServerError() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{nope"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginIgnoresEmailCaseAndStartsASession() throws Exception {
        postJson("/api/auth/register", VALID_REGISTRATION);

        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login(" ASHA@example.com ", "correct-horse")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Asha Rao"))
                .andReturn();

        assertThat(result.getResponse().getCookie(AuthCookie.NAME)).isNotNull();
    }

    @Test
    void wrongPasswordAndUnknownEmailGetTheSameResponse() throws Exception {
        postJson("/api/auth/register", VALID_REGISTRATION);

        MvcResult wrongPassword = postJson("/api/auth/login", login("asha@example.com", "wrong-password"));
        MvcResult unknownEmail = postJson("/api/auth/login", login("nobody@example.com", "wrong-password"));

        assertThat(wrongPassword.getResponse().getStatus()).isEqualTo(401);
        assertThat(unknownEmail.getResponse().getStatus()).isEqualTo(401);
        assertThat(wrongPassword.getResponse().getCookie(AuthCookie.NAME)).isNull();
        assertThat(wrongPassword.getResponse().getContentAsString())
                .isEqualTo(unknownEmail.getResponse().getContentAsString());
    }

    @Test
    void disabledAccountCannotLogIn() throws Exception {
        postJson("/api/auth/register", VALID_REGISTRATION);
        User user = users.findByEmail("asha@example.com").orElseThrow();
        user.setEnabled(false);
        users.saveAndFlush(user);

        assertThat(postJson("/api/auth/login", login("asha@example.com", "correct-horse"))
                        .getResponse()
                        .getStatus())
                .isEqualTo(401);
    }

    @Test
    void meReturnsTheSessionUser() throws Exception {
        Cookie session = postJson("/api/auth/register", VALID_REGISTRATION)
                .getResponse()
                .getCookie(AuthCookie.NAME);

        mvc.perform(get("/api/auth/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("asha@example.com"))
                .andExpect(jsonPath("$.phone").value("9876543210"));
    }

    @Test
    void logoutExpiresTheCookie() throws Exception {
        String header = mvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.SET_COOKIE);

        assertThat(header).contains(AuthCookie.NAME + "=", "Max-Age=0", "HttpOnly");
    }
}
