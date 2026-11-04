package com.swiftdrop.notification.security;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Read-only lookup in core-api's users table (same database).
 *
 * <p>The role and the enabled flag come from the table rather than from the token, so an account
 * that an admin disabled or whose role changed cannot keep using a token issued earlier. This is
 * what core-api's JwtTokenAuthenticationFilter does too.
 */
@Component
@RequiredArgsConstructor
public class UserDirectory {

    public record Account(Long id, String email, String role, boolean enabled) {}

    private final JdbcTemplate jdbc;

    public Optional<Account> findByEmail(String email) {
        return jdbc.query(
                "SELECT id, email, role, enabled FROM users WHERE email = ?",
                (rs, i) -> new Account(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getBoolean("enabled")),
                email
        ).stream().findFirst();
    }
}
