package com.swiftdrop.tracking.dto;

/**
 * Message pushed to /topic/tracking/{trackingNumber}.
 * Public: status and time only. Never a note, name, phone or address.
 */
public record TrackingUpdateMessage(String trackingNumber, String status, String changedAt) {}
