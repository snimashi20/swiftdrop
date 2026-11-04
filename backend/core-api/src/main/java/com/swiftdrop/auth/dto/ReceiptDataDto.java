package com.swiftdrop.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptDataDto {

    private String receiptId;
    private String trackingNumber;
    private String issueDate;

    // Sender details
    private String senderName;
    private String senderEmail;
    private String senderPhone;
    private String pickupAddress;

    // Recipient details
    private String receiverName;
    private String receiverPhone;
    private String dropoffAddress;

    // Parcel specifics
    private BigDecimal weightKg;
    private String size;
    private String currentStatus;
    private String serviceType;

    // Financial breakdown
    private BigDecimal deliveryFee;

    // Status timeline
    @Builder.Default
    private List<StatusTimelineEntry> statusHistory = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusTimelineEntry {
        private String status;
        private String changedAt;
        private String changedByRole;
        private String note;
    }
}
