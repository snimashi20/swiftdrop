package com.swiftdrop.dispatch.dto;

import com.swiftdrop.dispatch.domain.VehicleType;

/** Partial update: any field left null stays unchanged. */
public record CourierProfileUpdateRequest(
        VehicleType vehicleType,
        Long zoneId,
        Boolean active
) {}