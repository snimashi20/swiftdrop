package com.swiftdrop.courier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeliverParcelRequest {
    @NotBlank(message = "Recipient name is required")
    @Size(max = 100, message = "Recipient name cannot exceed 100 characters")
    private String recipientName;

    @Size(max = 500, message = "Note cannot exceed 500 characters")
    private String note;
}