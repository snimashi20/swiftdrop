package com.swiftdrop.webhook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestPingResponse {
    private boolean success;
    private int httpStatus;
    private String message;
    private long durationMs;
}
