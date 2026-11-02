package com.swiftdrop.parcel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ParcelAddressDto {

    @NotBlank(message = "Address line is required")
    @Size(max = 200)
    private String line1;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @NotBlank(message = "District is required")
    @Size(max = 100)
    private String district;
}