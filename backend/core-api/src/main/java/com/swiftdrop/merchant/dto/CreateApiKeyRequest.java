package com.swiftdrop.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateApiKeyRequest(
    @NotBlank(message = "API key label is required")
    @Size(max = 100, message = "API key label must not exceed 100 characters")
    String label
) {}
