package com.swiftdrop.pricing.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuoteResponse {
    private BigDecimal fee;
    private String pickupZone;
    private String dropoffZone;
    private Long pickupZoneId;
    private Long dropoffZoneId;
    private boolean isInterZone;
}
