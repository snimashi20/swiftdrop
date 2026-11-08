package com.swiftdrop.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusToggleDto {

    @NotNull(message = "enabled field is required")
    private Boolean enabled;

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }
}