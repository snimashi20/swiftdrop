package com.swiftdrop.merchant.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Shared, correctly-configured ObjectMapper for the API-key security handlers.
 * <p>
 * Mirrors the settings that Spring Boot's JacksonAutoConfiguration applies to
 * the application-wide mapper (JavaTimeModule registered,
 * WRITE_DATES_AS_TIMESTAMPS disabled) so that {@code ErrorResponse.timestamp}
 * is serialised as an ISO-8601 string, identical to what
 * {@link com.swiftdrop.common.exception.GlobalExceptionHandler} produces.
 * <p>
 * Kept package-private; only the two security handlers need it.
 */
final class SecurityJsonMapper {

    /** Single, shared, thread-safe instance. */
    static final ObjectMapper INSTANCE = build();

    private SecurityJsonMapper() {}

    private static ObjectMapper build() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
