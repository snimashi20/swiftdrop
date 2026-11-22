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

    @NotBlank(message = "Business name is required.")
    @Size(max = 150, message = "Business name must be 150 characters or fewer.")
    private String businessName;

    @NotBlank(message = "Business structure is required.")
    @Pattern(regexp = "SOLE_PROPRIETORSHIP|PARTNERSHIP|PRIVATE_LIMITED_COMPANY|OTHER",
            message = "Select a valid business structure.")
    private String businessType;

    @NotBlank(message = "Business registration number is required.")
    @Size(max = 100, message = "Business registration number must be 100 characters or fewer.")
    private String registrationNumber;

    @NotBlank(message = "Business address is required.")
    @Size(min = 5, max = 300, message = "Business address must be between 5 and 300 characters.")
    private String businessAddress;

    @NotBlank(message = "Business description is required.")
    @Size(min = 10, max = 500, message = "Business description must be between 10 and 500 characters.")
    private String businessDescription;

    @NotBlank(message = "Contact name is required.")
    @Size(max = 100, message = "Contact name must be 100 characters or fewer.")
    private String contactName;

    @NotBlank(message = "Contact phone number is required.")
    @Pattern(regexp = "^(?:0|\\+94)7[0-9]{8}$",
            message = "Enter a Sri Lankan mobile number such as 0771234567 or +94771234567.")
    private String contactPhone;

    @AssertTrue(message = "Confirm that you are authorized to represent this business and that the information is accurate.")
    private boolean verificationDeclarationAccepted;
}
