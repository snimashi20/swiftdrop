package com.swiftdrop.dispatch.dto;

import com.swiftdrop.dispatch.domain.VehicleType;

import java.time.Instant;

public record CourierProfileResponse(
        Long id,
        Long userId,
        VehicleType vehicleType,
        Long zoneId,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}