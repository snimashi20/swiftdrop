package com.swiftdrop.auth.service;

import com.swiftdrop.auth.dto.ReceiptDataDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReceiptServiceTest {

    private ReceiptService receiptService;

    @BeforeEach
    void setUp() {
        receiptService = new ReceiptService();
    }

    @Test
    @DisplayName("Generate receipt with full ReceiptDataDto returns valid PDF with %PDF- magic bytes")
    void generateDeliveryReceipt_withFullData_returnsValidPdf() {
        ReceiptDataDto dto = ReceiptDataDto.builder()
                .receiptId("RCPT-SD12345678AA")
                .trackingNumber("SD12345678AA")
                .issueDate("2026-11-04 14:00:00")
                .senderName("Sadeepa Perera")
                .senderPhone("0771234567")
                .senderEmail("sadeepa@example.com")
                .pickupAddress("No. 10, Galle Road, Colombo 03")
                .receiverName("Kasun Silva")
                .receiverPhone("0719876543")
                .dropoffAddress("No. 45, Peradeniya Rd, Kandy")
                .weightKg(new BigDecimal("3.25"))
                .size("MEDIUM")
                .currentStatus("DELIVERED")
                .serviceType("Standard Express")
                .deliveryFee(new BigDecimal("850.00"))
                .statusHistory(List.of(
                        ReceiptDataDto.StatusTimelineEntry.builder()
                                .status("CREATED")
                                .changedAt("2026-11-02 10:00:00")
                                .changedByRole("CUSTOMER")
                                .note("Parcel created online")
                                .build(),
                        ReceiptDataDto.StatusTimelineEntry.builder()
                                .status("PICKED_UP")
                                .changedAt("2026-11-02 14:30:00")
                                .changedByRole("COURIER")
                                .note("Picked up from sender")
                                .build(),
                        ReceiptDataDto.StatusTimelineEntry.builder()
                                .status("DELIVERED")
                                .changedAt("2026-11-03 16:15:00")
                                .changedByRole("COURIER")
                                .note("Handed over to Kasun")
                                .build()
                ))
                .build();

        byte[] pdfBytes = receiptService.generateDeliveryReceipt(dto);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF document should contain substantial content");

        // Verify PDF Header signature (%PDF)
        String header = new String(pdfBytes, 0, 4, StandardCharsets.US_ASCII);
        assertEquals("%PDF", header, "PDF document must begin with '%PDF' signature");
    }

    @Test
    @DisplayName("Generate receipt without status history still generates clean PDF")
    void generateDeliveryReceipt_withoutStatusHistory_generatesValidPdf() {
        ReceiptDataDto dto = ReceiptDataDto.builder()
                .receiptId("RCPT-SD99999999ZZ")
                .trackingNumber("SD99999999ZZ")
                .issueDate("2026-11-04 14:00:00")
                .senderName("Alice")
                .receiverName("Bob")
                .dropoffAddress("Galle")
                .weightKg(new BigDecimal("1.00"))
                .deliveryFee(new BigDecimal("500.00"))
                .build();

        byte[] pdfBytes = receiptService.generateDeliveryReceipt(dto);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        String header = new String(pdfBytes, 0, 4, StandardCharsets.US_ASCII);
        assertEquals("%PDF", header);
    }

    @Test
    @DisplayName("Legacy overloaded generateDeliveryReceipt signature generates valid PDF")
    void generateDeliveryReceipt_legacyMethod_returnsValidPdf() {
        byte[] pdfBytes = receiptService.generateDeliveryReceipt(
                "SD12345678AA",
                "Sender Name",
                "Receiver Name",
                "Colombo",
                2.5,
                750.0
        );

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        String header = new String(pdfBytes, 0, 4, StandardCharsets.US_ASCII);
        assertEquals("%PDF", header);
    }
}

