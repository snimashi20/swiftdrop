package com.swiftdrop.dispatch.dto;

import com.swiftdrop.dispatch.domain.VehicleType;
import jakarta.validation.constraints.NotNull;

public record CourierProfileCreateRequest(
        @NotNull Long userId,
        @NotNull VehicleType vehicleType,
        @NotNull Long zoneId,
        Boolean active          // optional, defaults to true
) {}