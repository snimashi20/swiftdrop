package com.swiftdrop.merchant.dto;

import java.time.Instant;

public record ApiKeyResponse(
    Long id,
    Long merchantId,
    String label,
    String keyPrefix,
    boolean active,
    Instant createdAt,
    Instant lastUsedAt,
    Instant revokedAt
) {}
