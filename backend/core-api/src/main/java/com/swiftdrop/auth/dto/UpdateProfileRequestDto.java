package com.swiftdrop.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateProfileRequestDto {

    @NotBlank(message = "Full name cannot be blank")
    private String fullName;

    @Pattern(
        regexp = "^(?:0|\\+94)7[0-9]{8}$",
        message = "Must be a valid Sri Lankan mobile number (e.g., 07XXXXXXXX or +947XXXXXXXX)"
    )
    private String phoneNumber;
}