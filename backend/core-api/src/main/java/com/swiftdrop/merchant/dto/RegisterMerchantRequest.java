package com.swiftdrop.merchant.dto;

import jakarta.validation.constraints.Email;
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

    @NotBlank(message = "Business registration number is required")
    @Size(max = 100, message = "Registration number cannot exceed 100 characters")
    private String registrationNumber;

    @NotBlank(message = "Contact name is required")
    @Size(max = 100, message = "Contact name cannot exceed 100 characters")
    private String contactName;

    @NotBlank(message = "Contact phone is required")
    @Pattern(
        regexp = "^(?:0|\\+94)7[0-9]{8}$",
        message = "Must be a valid Sri Lankan mobile number (e.g. 07XXXXXXXX or +947XXXXXXXX)"
    )
    private String contactPhone;
}
