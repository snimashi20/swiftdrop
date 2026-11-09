package com.swiftdrop.courier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierDeliveryResponse {
    private Long parcelId;
    private String trackingNumber;
    private String recipientName;
    private String recipientPhone;
    private String deliveryAddress;
    private String status;
    private BigDecimal weightKg;
    private String deliveryInstructions;
    private Instant assignedAt;
    private String proofPhotoUrl;
}