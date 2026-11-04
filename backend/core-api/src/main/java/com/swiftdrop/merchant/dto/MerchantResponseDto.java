package com.swiftdrop.merchant.dto;

import com.swiftdrop.merchant.domain.MerchantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantResponseDto {
    private Long id;
    private Long userId;
    private String businessName;
    private String registrationNumber;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private MerchantStatus status;
    private String rejectionReason;
    private Instant createdAt;
    private Instant reviewedAt;
    private Long reviewedBy;
}
