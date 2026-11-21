package com.swiftdrop.courier.dto;

import com.swiftdrop.courier.domain.CourierApplicationStatus;
import com.swiftdrop.dispatch.domain.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierApplicationResponseDto {
    private Long id;
    private Long userId;
    private String fullName;
    private String contactEmail;
    private String contactPhone;
    private String licenseNumber;
    private VehicleType vehicleType;
    private String preferredZone;
    private CourierApplicationStatus status;
    private String rejectionReason;
    private Instant createdAt;
    private Instant reviewedAt;
    private Long reviewedBy;
}
