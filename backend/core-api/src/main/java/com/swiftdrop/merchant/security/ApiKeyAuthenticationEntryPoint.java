package com.swiftdrop.merchant.security;

import com.swiftdrop.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Returns a 401 JSON error when no valid API key is present on the
 * {@code /api/v1/merchant-api/**} filter chain.
 *
 * <p>Uses {@link SecurityJsonMapper#INSTANCE} – a self-contained, correctly
 * configured ObjectMapper (JavaTimeModule + ISO-8601 timestamps) – so the
 * serialised body is identical to what
 * {@link com.swiftdrop.common.exception.GlobalExceptionHandler} produces,
 * without introducing any Spring dependency on this class.
 */
@Component
public class ApiKeyAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "INVALID_API_KEY",
                "Invalid or missing API key",
                request.getRequestURI()
        );

        SecurityJsonMapper.INSTANCE.writeValue(response.getOutputStream(), errorResponse);
    }
}
