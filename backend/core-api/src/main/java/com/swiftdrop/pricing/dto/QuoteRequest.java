package com.swiftdrop.pricing.dto;

import com.swiftdrop.parcel.domain.ParcelSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class QuoteRequest {
    @NotBlank(message = "Pickup district is required")
    private String pickupDistrict;

    @NotBlank(message = "Dropoff district is required")
    private String dropoffDistrict;

    @NotNull(message = "Size is required")
    private ParcelSize size;

    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be greater than zero")
    private BigDecimal weightKg;
}
