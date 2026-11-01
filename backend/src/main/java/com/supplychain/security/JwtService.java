package com.supplychain.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Issues and verifies session tokens. Tokens carry only the user ID; roles are read from the database. */
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration expiry;
    private final Clock clock;

    public JwtService(JwtProperties properties, Clock clock) {
        // Rejects secrets under 32 bytes, which stops the application at startup.
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expiry = properties.expiry();
        this.clock = clock;
    }

    public String issue(long userId) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(Long.toString(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiry)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** Returns the user ID, or empty if the token is malformed, tampered with or expired. */
    public Optional<Long> verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(Long.valueOf(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public Duration getExpiry() {
        return expiry;
    }
}
