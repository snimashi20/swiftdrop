package com.swiftdrop.parcel.dto;

import com.swiftdrop.parcel.domain.ParcelStatus;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParcelResponse {
    private Long id;
    private String trackingNumber;
    private ParcelStatus status;
    private BigDecimal fee;
    private Instant createdAt;
}