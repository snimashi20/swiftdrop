package com.swiftdrop.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET = "01234567890123456789012345678901";

    @Test
    void generatesAndParsesExpectedClaims() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 60_000);

        String token = provider.generateToken(7L, "user@example.com", Set.of("ROLE_CUSTOMER"));
        Claims claims = provider.parseClaims(token);

        assertThat(claims.get("userId", Long.class)).isEqualTo(7L);
        assertThat(claims.get("email", String.class)).isEqualTo("user@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_CUSTOMER");
        assertThat(claims.get("authorities", java.util.List.class)).contains("ROLE_CUSTOMER");
        assertThat(claims.getSubject()).isEqualTo("user@example.com");
        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @Test
    void rejectsExpiredToken() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, -1);
        String token = provider.generateToken(7L, "user@example.com", Set.of("ROLE_CUSTOMER"));

        assertThat(provider.isTokenValid(token)).isFalse();
        assertThatThrownBy(() -> provider.parseClaims(token)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void rejectsInvalidSignature() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 60_000);
        SecretKey otherKey = Keys.hmacShaKeyFor("12345678901234567890123456789012".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder().subject("user@example.com").signWith(otherKey).compact();

        assertThat(provider.isTokenValid(token)).isFalse();
        assertThatThrownBy(() -> provider.parseClaims(token)).isInstanceOf(RuntimeException.class);
    }
}
