package com.swiftdrop.pricing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ZoneResponse {
    private Long id;
    private String name;
    private String district;

    @JsonProperty("isActive")
    private boolean isActive;
}
