package com.swiftdrop.merchant.security;

import com.swiftdrop.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Returns a 403 JSON error when a merchant whose status is not APPROVED
 * reaches the {@code /api/v1/merchant-api/**} filter chain.
 *
 * <p>Uses {@link SecurityJsonMapper#INSTANCE} – a self-contained, correctly
 * configured ObjectMapper (JavaTimeModule + ISO-8601 timestamps) – so the
 * serialised body is identical to what
 * {@link com.swiftdrop.common.exception.GlobalExceptionHandler} produces,
 * without introducing any Spring dependency on this class.
 */
@Component
public class ApiKeyAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                "MERCHANT_NOT_APPROVED",
                "Merchant is not approved",
                request.getRequestURI()
        );

        SecurityJsonMapper.INSTANCE.writeValue(response.getOutputStream(), errorResponse);
    }
}
