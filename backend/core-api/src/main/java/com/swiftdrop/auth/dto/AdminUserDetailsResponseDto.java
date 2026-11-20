package com.swiftdrop.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailsResponseDto {
    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String role;
    private String provider;
    private boolean enabled;
    private String status;
    private OffsetDateTime createdAt;

    // Optional Merchant application data
    private MerchantDetailsDto merchantProfile;

    // Optional Courier application data
    private CourierDetailsDto courierProfile;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MerchantDetailsDto {
        private Long id;
        private String businessName;
        private String registrationNumber;
        private String contactName;
        private String contactEmail;
        private String contactPhone;
        private String status;
        private String rejectionReason;
        private String createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourierDetailsDto {
        private Long id;
        private String vehicleType;
        private Long zoneId;
        private String zoneName;
        private boolean active;
        private String nicNumber;
        private String dateOfBirth;
        private String residentialAddress;
        private String drivingLicenseNumber;
        private String drivingLicenseExpiry;
        private String vehiclePlateNumber;
        private String emergencyContactName;
        private String emergencyContactPhone;
        private String nicFrontPhotoUrl;
        private String nicBackPhotoUrl;
        private String createdAt;
    }
}

