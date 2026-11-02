package com.swiftdrop.tracking.dto;

import com.swiftdrop.parcel.domain.ParcelStatus;

import java.time.Instant;
import java.util.List;

/** Public tracking view: status and timeline only, personal data hidden. */
public record TrackingResponse(
        String trackingNumber,
        ParcelStatus status,
        String receiverName,   // short form, e.g. "N. Perera"
        String pickupCity,     // city only
        String dropoffCity,    // city only
        List<Entry> timeline) {

    public record Entry(ParcelStatus status, Instant changedAt) {}
}
