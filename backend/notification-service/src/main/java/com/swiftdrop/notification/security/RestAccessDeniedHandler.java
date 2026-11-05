package com.swiftdrop.notification.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Answers a valid token without the required role with 403 JSON. */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        JsonErrorWriter.write(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN",
                "Your role is not allowed to use this endpoint.", request.getRequestURI());
    }
}
