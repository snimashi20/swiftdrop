package com.swiftdrop.notification.common;

import java.time.Instant;

/** The same error body core-api returns, so one frontend error handler covers both services. */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path
) {
}
