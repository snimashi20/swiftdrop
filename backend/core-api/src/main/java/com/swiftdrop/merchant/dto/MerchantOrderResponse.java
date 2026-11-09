package com.swiftdrop.merchant.dto;

import com.swiftdrop.parcel.domain.ParcelStatus;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MerchantOrderResponse {
    private String trackingNumber;
    private ParcelStatus status;
    private BigDecimal fee;
    private Instant createdAt;
    private String externalOrderId;
}
