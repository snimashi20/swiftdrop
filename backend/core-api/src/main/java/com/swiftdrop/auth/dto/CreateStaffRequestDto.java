package com.swiftdrop.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateStaffRequestDto {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @Pattern(regexp = "^(?:0|\\+94)7[0-9]{8}$", message = "Must be a valid Sri Lankan mobile number (e.g., 07XXXXXXXX or +947XXXXXXXX)")
    private String phoneNumber;

    @NotBlank(message = "Role is required (COURIER or DISPATCHER)")
    private String role; // COURIER or DISPATCHER

    @NotBlank(message = "Password is required for staff account creation")
    private String password;
}