package com.swiftdrop.notification.api;

import com.swiftdrop.notification.common.ErrorResponse;
import com.swiftdrop.notification.common.NotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The error body every notification endpoint returns. It must match core-api's shape
 * (timestamp, status, code, message, path) so one frontend handler covers both services.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("a missing notification answers 404 NOT_FOUND with the request path")
    void notFoundIs404() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/notifications/5/read");

        ResponseEntity<ErrorResponse> response =
                handler.handleNotFound(new NotFoundException("No notification 5 for this account."), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(404);
        assertThat(body.code()).isEqualTo("NOT_FOUND");
        assertThat(body.message()).isEqualTo("No notification 5 for this account.");
        assertThat(body.path()).isEqualTo("/api/v1/notifications/5/read");
        assertThat(body.timestamp()).isNotNull().isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("a page size outside its range answers 400 BAD_REQUEST with the violation message")
    void constraintViolationIs400() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/notifications/me");

        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(
                new ConstraintViolationException("size must be less than or equal to 100", Set.of()), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.code()).isEqualTo("BAD_REQUEST");
        assertThat(body.message()).contains("size must be less than or equal to 100");
        assertThat(body.path()).isEqualTo("/api/v1/notifications/me");
    }

    @Test
    @DisplayName("an unknown filter value answers 400 BAD_REQUEST naming the parameter, not the raw value")
    void typeMismatchIs400AndNamesTheParameter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/notifications/admin");

        ResponseEntity<ErrorResponse> response =
                handler.handleTypeMismatch(typeMismatch("type", "CARRIER_PIGEON"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.code()).isEqualTo("BAD_REQUEST");
        assertThat(body.message()).isEqualTo("Invalid value for 'type'.");
        assertThat(body.message()).doesNotContain("CARRIER_PIGEON");
        assertThat(body.path()).isEqualTo("/api/v1/notifications/admin");
    }

    @Test
    @DisplayName("a non-numeric notification id answers 400 BAD_REQUEST naming the id parameter")
    void nonNumericIdIs400() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/notifications/abc/read");

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(typeMismatch("id", "abc"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Invalid value for 'id'.");
    }

    private static MethodArgumentTypeMismatchException typeMismatch(String name, Object value) throws Exception {
        Method method = Target.class.getDeclaredMethod("handle", Long.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        return new MethodArgumentTypeMismatchException(value, Long.class, name, parameter,
                new NumberFormatException("For input string: " + value));
    }

    /** Only a source of a real MethodParameter for the exception under test. */
    @SuppressWarnings("unused")
    private static final class Target {
        void handle(Long id) {
        }
    }
}
