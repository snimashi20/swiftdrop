package com.swiftdrop.notification.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verification of the tokens core-api issues. This service never mints one, so the cases that
 * matter are: a token signed with the shared secret is accepted, and everything else is refused.
 */
class JwtServiceTest {

    /** The same secret the test profile puts in app.jwt.secret. */
    private static final String SECRET = "test-secret-key-for-unit-tests-only-min-32-chars!!";
    private static final String OTHER_SECRET = "a-completely-different-secret-key-of-32-chars-plus!!";

    private final JwtService jwtService = new JwtService(SECRET);

    @Test
    @DisplayName("a token signed with the shared secret is parsed with all of its claims")
    void validTokenIsParsed() {
        String token = token(SECRET, 3_600_000);

        Claims claims = jwtService.extractClaims(token);

        assertThat(claims.getSubject()).isEqualTo("7");
        assertThat(claims.get("email", String.class)).isEqualTo("sithmi@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("CUSTOMER");
        assertThat(claims.get("userId", Number.class).longValue()).isEqualTo(7L);
        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @Test
    @DisplayName("an expired token is refused")
    void expiredTokenIsRefused() {
        String expired = token(SECRET, -60_000);

        assertThatThrownBy(() -> jwtService.extractClaims(expired))
                .isInstanceOf(ExpiredJwtException.class)
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("a token signed with another secret is refused")
    void tokenSignedWithAnotherKeyIsRefused() {
        String foreign = token(OTHER_SECRET, 3_600_000);

        assertThatThrownBy(() -> jwtService.extractClaims(foreign))
                .isInstanceOf(SignatureException.class)
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("a made-up token string is refused")
    void malformedTokenIsRefused() {
        assertThatThrownBy(() -> jwtService.extractClaims("not.a.token"))
                .isInstanceOf(MalformedJwtException.class)
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("an unsigned token is refused, so claims cannot be forged by dropping the signature")
    void unsignedTokenIsRefused() {
        String unsigned = Jwts.builder()
                .subject("7")
                .claim("role", "ADMIN")
                .compact();

        assertThatThrownBy(() -> jwtService.extractClaims(unsigned))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("a token whose payload was tampered with is refused")
    void tamperedTokenIsRefused() {
        String token = token(SECRET, 3_600_000);
        String tampered = token.substring(0, token.lastIndexOf('.')) + ".ZmFrZS1zaWduYXR1cmU";

        assertThatThrownBy(() -> jwtService.extractClaims(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("an empty token is refused rather than treated as anonymous")
    void emptyTokenIsRefused() {
        assertThatThrownBy(() -> jwtService.extractClaims(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("a secret shorter than the HMAC-SHA256 minimum is refused at startup")
    void tooShortSecretIsRefusedAtStartup() {
        assertThatThrownBy(() -> new JwtService("too-short"))
                .isInstanceOf(WeakKeyException.class);
    }

    private static String token(String secret, long millisUntilExpiry) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Date issuedAt = new Date(System.currentTimeMillis() - 120_000);
        return Jwts.builder()
                .subject("7")
                .claim("email", "sithmi@example.com")
                .claim("role", "CUSTOMER")
                .claim("userId", 7L)
                .issuedAt(issuedAt)
                .expiration(new Date(System.currentTimeMillis() + millisUntilExpiry))
                .signWith(key)
                .compact();
    }
}
