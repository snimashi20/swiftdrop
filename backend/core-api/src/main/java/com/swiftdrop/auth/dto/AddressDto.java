package com.swiftdrop.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto {
    private Long id;

    private String addressLabel;

    @NotBlank(message = "Street address is required")
    private String streetAddress;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "District is required")
    private String district;

    private String postalCode;

    @JsonProperty("isDefault")
    @JsonAlias({"default", "isDefault"})
    @Builder.Default
    private Boolean isDefault = false;

    public boolean isDefault() {
        return Boolean.TRUE.equals(isDefault);
    }
}