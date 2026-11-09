package com.swiftdrop.courier.dto;

import com.swiftdrop.courier.domain.FailureReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FailParcelRequest {
    @NotNull(message = "Failure reason is required")
    private FailureReason reason;

    @Size(max = 500, message = "Note cannot exceed 500 characters")
    private String note;
}