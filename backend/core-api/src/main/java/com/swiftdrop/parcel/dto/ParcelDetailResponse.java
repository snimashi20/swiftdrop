package com.swiftdrop.parcel.dto;

import com.swiftdrop.parcel.domain.ParcelSize;
import com.swiftdrop.parcel.domain.ParcelStatus;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParcelDetailResponse {
    private Long id;
    private String trackingNumber;
    private ParcelStatus status;
    private BigDecimal fee;
    private ParcelAddressDto pickup;
    private ParcelAddressDto dropoff;
    private String receiverName;
    private String receiverPhone;
    private ParcelSize size;
    private BigDecimal weightKg;
    private String deliveryInstructions;
    private Instant createdAt;
    private Instant updatedAt;
}