package com.swiftdrop.parcel.dto;

import com.swiftdrop.parcel.domain.ParcelSize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class CreateParcelRequest {

    @NotNull(message = "Pickup address is required")
    @Valid
    private ParcelAddressDto pickup;

    @NotNull(message = "Dropoff address is required")
    @Valid
    private ParcelAddressDto dropoff;

    @NotBlank(message = "Receiver name is required")
    @Size(max = 100)
    private String receiverName;

    @NotBlank(message = "Receiver phone is required")
    @Size(max = 20)
    private String receiverPhone;

    @NotNull(message = "Size is required")
    private ParcelSize size;

    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be greater than zero")
    @DecimalMax(value = "9999.99", message = "Weight is too large")
    private BigDecimal weightKg;

    @Size(max = 500)
    private String deliveryInstructions;
}