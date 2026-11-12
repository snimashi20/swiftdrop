package com.swiftdrop.webhook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class UpdateWebhookEndpointRequest {

    @NotBlank(message = "Webhook URL must not be blank")
    @Size(max = 500, message = "Webhook URL cannot exceed 500 characters")
    @Pattern(regexp = "^https?://.+", message = "Webhook URL must start with http:// or https://")
    private String url;

    @NotNull(message = "Active status must be specified")
    private Boolean active;
}
