package com.swiftdrop.dispatch.dto;

import com.swiftdrop.parcel.domain.ParcelStatus;

import java.time.Instant;

public record ParcelAssignmentResponse(
        Long parcelId,
        String trackingNumber,
        ParcelStatus status,
        Long assignedCourierId,
        Long assignedByUserId,
        Instant assignedAt
) {}