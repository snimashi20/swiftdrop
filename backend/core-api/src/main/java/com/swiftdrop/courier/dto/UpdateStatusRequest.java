package com.swiftdrop.courier.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateStatusRequest {
    @NotBlank(message = "New status is required")
    private String status;

    private String note;
}