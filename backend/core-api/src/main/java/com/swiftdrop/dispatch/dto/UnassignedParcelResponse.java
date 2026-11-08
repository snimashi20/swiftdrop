package com.swiftdrop.dispatch.dto;

import com.swiftdrop.parcel.domain.ParcelSize;
import com.swiftdrop.parcel.domain.ParcelStatus;

import java.math.BigDecimal;
import java.time.Instant;

/** No receiver name, phone or street address: the dispatcher only needs the areas. */
public record UnassignedParcelResponse(
        Long id,
        String trackingNumber,
        ParcelStatus status,
        String pickupCity,
        String pickupDistrict,
        String dropoffCity,
        String dropoffDistrict,
        ParcelSize size,
        BigDecimal weightKg,
        Instant createdAt
) {}