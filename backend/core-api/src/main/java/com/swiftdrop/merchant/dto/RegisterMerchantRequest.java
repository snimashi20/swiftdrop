package com.swiftdrop.merchant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterMerchantRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Business name is required")
    @Size(max = 150, message = "Business name cannot exceed 150 characters")
    private String businessName;

    @NotBlank(message = "Business type is required")
    @Pattern(regexp = "SOLE_PROPRIETORSHIP|PARTNERSHIP|PRIVATE_LIMITED_COMPANY|OTHER",
        message = "Select a supported business type")
    private String businessType;

    @NotBlank(message = "Business registration number is required")
    @Size(max = 100, message = "Registration number cannot exceed 100 characters")
    private String registrationNumber;

    @NotBlank(message = "Business address is required")
    @Size(min = 5, max = 300, message = "Business address must be between 5 and 300 characters")
    private String businessAddress;

    @NotBlank(message = "Business activity description is required")
    @Size(min = 10, max = 500, message = "Business activity description must be between 10 and 500 characters")
    private String businessDescription;

    @NotBlank(message = "Contact name is required")
    @Size(max = 100, message = "Contact name cannot exceed 100 characters")
    private String contactName;

    @NotBlank(message = "Contact phone is required")
    @Pattern(
        regexp = "^(?:0|\\+94)7[0-9]{8}$",
        message = "Must be a valid Sri Lankan mobile number (e.g. 07XXXXXXXX or +947XXXXXXXX)"
    )
    private String contactPhone;

    @AssertTrue(message = "You must confirm that the business information is accurate")
    private boolean verificationDeclarationAccepted;
}
