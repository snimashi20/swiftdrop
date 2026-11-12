package com.swiftdrop.webhook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEndpointDto {
    private Long id;
    private Long merchantId;
    private String url;
    private String secret;
    private Boolean active;
    private Instant createdAt;
}
