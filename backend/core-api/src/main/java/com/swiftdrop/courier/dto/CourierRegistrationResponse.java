package com.swiftdrop.courier.dto;

import lombok.Builder;

@Builder
public record CourierRegistrationResponse(
        Long userId,
        Long courierProfileId,
        String fullName,
        String email,
        String vehicleType,
        Long zoneId,
        String status,
        String nicFrontPhotoUrl,
        String nicBackPhotoUrl
) {}