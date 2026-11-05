package com.swiftdrop.pricing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ZoneRequest {
    @NotBlank(message = "Zone name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "District is required")
    @Size(max = 100)
    private String district;
}
