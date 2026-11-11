package com.swiftdrop.dispatch.dto;

import com.swiftdrop.parcel.domain.ParcelSize;
import com.swiftdrop.parcel.domain.ParcelStatus;

import java.math.BigDecimal;
import java.time.Instant;

/** A parcel that already has a courier. No receiver name, phone or street address. */
public record AssignedParcelResponse(
        Long id,
        String trackingNumber,
        ParcelStatus status,
        String pickupCity,
        String pickupDistrict,
        String dropoffCity,
        String dropoffDistrict,
        ParcelSize size,
        BigDecimal weightKg,
        Long assignedCourierId,
        String courierName,
        Instant assignedAt
) {}