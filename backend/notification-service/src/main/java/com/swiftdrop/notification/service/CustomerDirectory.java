package com.swiftdrop.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

// Read-only lookup in core-api's users table (same database); the status event carries only customerId.
@Component
@RequiredArgsConstructor
public class CustomerDirectory {

    public record CustomerContact(String email, String fullName, String phoneNumber) {}

    private final JdbcTemplate jdbc;

    public Optional<CustomerContact> findById(Long customerId) {
        return jdbc.query(
                "SELECT email, full_name, phone_number FROM users WHERE id = ?",
                (rs, i) -> new CustomerContact(rs.getString("email"), rs.getString("full_name"), rs.getString("phone_number")),
                customerId
        ).stream().findFirst();
    }
}
