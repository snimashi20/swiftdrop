package com.swiftdrop.notification.security;

/**
 * The principal of an authenticated request. The id is the users row id, which is also the
 * customerId carried by a ParcelStatusChangedEvent, so a controller can filter the log by it
 * without another lookup.
 */
public record AuthenticatedUser(Long id, String email, String role) {
}
