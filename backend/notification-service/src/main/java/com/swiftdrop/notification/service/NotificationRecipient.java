package com.swiftdrop.notification.service;

/**
 * Someone a status change has to reach, with the contact details the channels need.
 *
 * <p>{@code email} and {@code phoneNumber} may be null or blank; only the channels
 * {@link NotificationPolicy} grants the role are ever attempted, and a channel whose contact detail
 * is missing is skipped rather than failed.
 */
public record NotificationRecipient(Long userId, String role, String email, String phoneNumber) {

    public static final String CUSTOMER = "CUSTOMER";
    public static final String MERCHANT = "MERCHANT";
    public static final String COURIER = "COURIER";
    public static final String DISPATCHER = "DISPATCHER";
    public static final String ADMIN = "ADMIN";
}
