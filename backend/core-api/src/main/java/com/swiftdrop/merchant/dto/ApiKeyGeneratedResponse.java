package com.swiftdrop.merchant.dto;

import java.time.Instant;

public record ApiKeyGeneratedResponse(
    Long id,
    Long merchantId,
    String label,
    String keyPrefix,
    String apiKey,
    boolean active,
    Instant createdAt
) {}
