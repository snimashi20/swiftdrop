package com.swiftdrop.merchant.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMerchantProfileRequest {

    @NotBlank
    @Size(max = 150)
    private String businessName;

    @NotBlank
    @Pattern(regexp = "SOLE_PROPRIETORSHIP|PARTNERSHIP|PRIVATE_LIMITED_COMPANY|OTHER")
    private String businessType;

    @NotBlank
    @Size(max = 100)
    private String registrationNumber;

    @NotBlank
    @Size(min = 5, max = 300)
    private String businessAddress;

    @NotBlank
    @Size(min = 10, max = 500)
    private String businessDescription;

    @NotBlank
    @Size(max = 100)
    private String contactName;

    @NotBlank
    @Pattern(regexp = "^(?:0|\\+94)7[0-9]{8}$")
    private String contactPhone;

    @AssertTrue
    private boolean verificationDeclarationAccepted;
}
