package com.swiftdrop.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleDto {

    @NotBlank(message = "Role is required")
    @Pattern(
        regexp = "^(?i)(CUSTOMER|MERCHANT|COURIER|DISPATCHER|ADMIN)$",
        message = "Role must be one of: CUSTOMER, MERCHANT, COURIER, DISPATCHER, ADMIN"
    )
    private String role;
}
