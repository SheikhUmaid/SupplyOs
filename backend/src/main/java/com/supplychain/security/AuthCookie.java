package com.supplychain.security;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * The session cookie. HttpOnly keeps the token out of JavaScript's reach. SameSite=Lax stops browsers
 * sending it on cross-site POST, PUT, PATCH or DELETE requests, which is the CSRF protection here.
 */
@Component
public class AuthCookie {

    public static final String NAME = "scos_token";

    private final boolean secure;

    public AuthCookie(@Value("${app.security.secure-cookie}") boolean secure) {
        this.secure = secure;
    }

    public ResponseCookie create(String token, Duration maxAge) {
        return base(token).maxAge(maxAge).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/");
    }
}
