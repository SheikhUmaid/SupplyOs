package com.supplychain.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-at-least-32-bytes";
    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");

    private JwtService serviceAt(Instant now, String secret) {
        return new JwtService(new JwtProperties(secret, Duration.ofHours(8)), Clock.fixed(now, ZoneOffset.UTC));
    }

    private final JwtService service = serviceAt(NOW, SECRET);

    @Test
    void roundTripsTheUserId() {
        assertThat(service.verify(service.issue(42))).contains(42L);
    }

    @Test
    void acceptsATokenUntilItExpires() {
        String token = service.issue(42);
        assertThat(serviceAt(NOW.plus(Duration.ofHours(7)), SECRET).verify(token)).contains(42L);
        assertThat(serviceAt(NOW.plus(Duration.ofHours(9)), SECRET).verify(token)).isEmpty();
    }

    @Test
    void rejectsATamperedPayload() {
        String[] parts = service.issue(42).split("\\.");
        String forged = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
                        .replace("\"42\"", "\"1\"")
                        .getBytes(StandardCharsets.UTF_8));
        assertThat(service.verify(parts[0] + "." + forged + "." + parts[2])).isEmpty();
    }

    @Test
    void rejectsATokenSignedWithAnotherKey() {
        String other = serviceAt(NOW, "a-different-secret-that-is-also-32-bytes-long").issue(42);
        assertThat(service.verify(other)).isEmpty();
    }

    @Test
    void rejectsAnUnsignedToken() {
        String unsigned = Jwts.builder().subject("42").expiration(Date.from(NOW.plusSeconds(60))).compact();
        assertThat(service.verify(unsigned)).isEmpty();
    }

    @Test
    void rejectsASignedTokenWhoseSubjectIsNotAnId() {
        String token = Jwts.builder()
                .subject("not-a-number")
                .expiration(Date.from(NOW.plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
        assertThat(service.verify(token)).isEmpty();
    }

    @Test
    void rejectsGarbage() {
        assertThat(service.verify("not-a-token")).isEmpty();
        assertThat(service.verify("")).isEmpty();
    }

    @Test
    void refusesToStartWithAShortSecret() {
        assertThatThrownBy(() -> serviceAt(NOW, "too-short")).isInstanceOf(WeakKeyException.class);
    }
}
