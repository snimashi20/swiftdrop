package com.swiftdrop.courier.dto;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.dispatch.domain.CourierProfile;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CourierFullProfileResponse {
    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String role;
    private String provider;
    private String status;
    private boolean active;

    // Onboarding Details
    private String nicNumber;
    private LocalDate dateOfBirth;
    private String residentialAddress;
    private String vehicleType;
    private String vehiclePlateNumber;
    private String drivingLicenseNumber;
    private LocalDate drivingLicenseExpiry;
    private Long zoneId;
    private String zoneName;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String nicFrontPhotoUrl;
    private String nicBackPhotoUrl;

    public static CourierFullProfileResponse from(User user, CourierProfile profile, String zoneName) {
    return CourierFullProfileResponse.builder()
            .id(profile.getId())
            .userId(user.getId())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .phoneNumber(user.getPhoneNumber())
            .role(user.getRole())
            .provider(user.getProvider() != null ? user.getProvider() : "LOCAL")
            .status(user.getStatus())
            .active(profile.isActive())
            .nicNumber(profile.getNicNumber())
            .dateOfBirth(profile.getDateOfBirth())
            .residentialAddress(profile.getResidentialAddress())
            .vehicleType(profile.getVehicleType() != null ? profile.getVehicleType().name() : null)
            .vehiclePlateNumber(profile.getVehiclePlateNumber())
            .drivingLicenseNumber(profile.getDrivingLicenseNumber())
            .drivingLicenseExpiry(profile.getDrivingLicenseExpiry())
            .zoneId(profile.getZoneId())
            .zoneName(zoneName)
            .emergencyContactName(profile.getEmergencyContactName())
            .emergencyContactPhone(profile.getEmergencyContactPhone())
            .nicFrontPhotoUrl(profile.getNicFrontPhotoUrl())
            .nicBackPhotoUrl(profile.getNicBackPhotoUrl())
            .build();
    }
}