package com.swiftdrop.notification.dto;

/** How many log rows the request marked as read; one status change can cover two rows. */
public record MarkedReadResponse(int updated) {
}
