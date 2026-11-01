package com.swiftdrop.dispatch.dto;

import com.swiftdrop.dispatch.domain.VehicleType;

public record CourierWorkloadResponse(
        Long courierProfileId,
        Long userId,
        VehicleType vehicleType,
        Long zoneId,
        boolean active,
        long activeDeliveries
) {}