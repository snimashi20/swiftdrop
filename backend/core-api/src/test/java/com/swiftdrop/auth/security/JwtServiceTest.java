package com.swiftdrop.auth.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "swiftdrop-dev-secret-key-change-me-in-production-min-32-chars!!";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION_MS);
    }

    @Test
    @DisplayName("Generates and validates real signed JWT successfully")
    void generateAndValidateToken_success() {
        String token = jwtService.generateToken(1L, "user@swiftdrop.lk", "Test User", "CUSTOMER");

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));

        Claims claims = jwtService.extractClaims(token);
        assertEquals("1", claims.getSubject());
        assertEquals("user@swiftdrop.lk", claims.get("email"));
        assertEquals("Test User", claims.get("name"));
        assertEquals("CUSTOMER", claims.get("role"));
        assertEquals(1, claims.get("userId", Integer.class));
    }

    @Test
    @DisplayName("Fails validation for tampered or invalid token")
    void validateToken_withTamperedToken_returnsFalse() {
        String token = jwtService.generateToken(1L, "user@swiftdrop.lk", "Test User", "CUSTOMER");
        String tamperedToken = token + "xyz";

        assertFalse(jwtService.validateToken(tamperedToken));
    }

    @Test
    @DisplayName("Fails validation for completely malformed token string")
    void validateToken_withMalformedString_returnsFalse() {
        assertFalse(jwtService.validateToken("not-a-valid-jwt"));
    }
}
