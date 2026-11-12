package com.swiftdrop.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record SelectRoleRequest(
        @NotBlank(message = "Role is required")
        String role
) {
}

