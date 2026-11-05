package com.swiftdrop.notification.api;

import com.swiftdrop.notification.common.ErrorResponse;
import com.swiftdrop.notification.common.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

/** Same error body as core-api: timestamp, status, code, message, path. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage(), request);
    }

    /** A page or size outside its allowed range. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e,
                                                                  HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage(), request);
    }

    /** An unknown value for type or status, or a non-numeric id. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e,
                                                            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                "Invalid value for '" + e.getName() + "'.", request);
    }

    // No handler for Exception on purpose: it would also catch AccessDeniedException from
    // @PreAuthorize and Spring's own 404 and 405, and answer 500 instead of the right status.

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message,
                                                HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                Instant.now(), status.value(), code, message, request.getRequestURI()));
    }
}
