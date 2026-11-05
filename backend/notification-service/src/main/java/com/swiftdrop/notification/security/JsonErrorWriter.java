package com.swiftdrop.notification.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.Instant;

/**
 * Writes the same error body as core-api's ErrorResponse.
 *
 * <p>Security handlers run outside the MVC stack, so there is no message converter here and the
 * few fields are written by hand.
 */
final class JsonErrorWriter {

    private JsonErrorWriter() {}

    static void write(HttpServletResponse response, int status, String code, String message, String path)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"timestamp\":\"" + Instant.now()
                        + "\",\"status\":" + status
                        + ",\"code\":\"" + code
                        + "\",\"message\":\"" + escape(message)
                        + "\",\"path\":\"" + escape(path) + "\"}");
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
