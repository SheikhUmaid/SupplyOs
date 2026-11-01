package com.supplychain;

import com.supplychain.security.AuthCookie;
import com.supplychain.security.JwtService;
import com.supplychain.user.User;
import com.supplychain.user.UserRepository;
import com.supplychain.user.UserRole;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.test.context.TestComponent;

/** Creates users and their session cookies for tests. */
@TestComponent
public class TestSessions {

    private final UserRepository users;
    private final JwtService jwtService;

    public TestSessions(UserRepository users, JwtService jwtService) {
        this.users = users;
        this.jwtService = jwtService;
    }

    public User user(UserRole role) {
        return users.saveAndFlush(new User(
                "Test " + role, UUID.randomUUID() + "@example.com", "unused-hash", null, role, Instant.now()));
    }

    public Cookie cookie(User user) {
        return new Cookie(AuthCookie.NAME, jwtService.issue(user.getId()));
    }

    public Cookie admin() {
        return cookie(user(UserRole.ADMIN));
    }

    public Cookie customer() {
        return cookie(user(UserRole.CUSTOMER));
    }
}
