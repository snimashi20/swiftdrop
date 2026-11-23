package com.swiftdrop.pricing.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ZoneResponse {
    private Long id;
    private String name;
    private String district;
    private boolean isActive;
}
