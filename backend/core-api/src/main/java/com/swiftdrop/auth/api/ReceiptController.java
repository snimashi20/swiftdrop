package com.swiftdrop.auth.api;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.dto.ReceiptDataDto;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.auth.service.ReceiptService;
import com.swiftdrop.common.exception.ForbiddenException;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.parcel.domain.Address;
import com.swiftdrop.parcel.domain.Parcel;
import com.swiftdrop.parcel.repository.ParcelRepository;
import com.swiftdrop.tracking.domain.StatusHistory;
import com.swiftdrop.tracking.repository.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;
    private final UserRepository userRepository;
    private final ParcelRepository parcelRepository;
    private final StatusHistoryRepository statusHistoryRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * GET /api/v1/parcels/{id}/receipt
     * Download delivery receipt PDF by parcel ID or tracking number.
     * Protected by strict IDOR check: Only the parcel owner or ADMIN may download.
     */
    @GetMapping("/api/v1/parcels/{id}/receipt")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> getParcelReceipt(@PathVariable String id) {
        return buildReceiptResponse(id);
    }

    /**
     * GET /api/v1/receipts/{trackingNumber}
     * Backward-compatible alias for existing frontend/test calls.
     */
    @GetMapping("/api/v1/receipts/{trackingNumber}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable String trackingNumber) {
        return buildReceiptResponse(trackingNumber);
    }

    private ResponseEntity<byte[]> buildReceiptResponse(String identifier) {
        User currentUser = getAuthenticatedUser();

        Parcel parcel = findParcelByIdOrTrackingNumber(identifier);

        // IDOR Defense: Only the parcel owner or an ADMIN user may access the receipt
        boolean isOwner = parcel.getCustomerId() != null && parcel.getCustomerId().equals(currentUser.getId());
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException(
                    "IDOR Violation: You do not have permission to download receipt for parcel: " + identifier);
        }

        // Fetch sender customer information if available
        String senderName = currentUser.getFullName();
        String senderPhone = currentUser.getPhoneNumber();
        String senderEmail = currentUser.getEmail();

        if (parcel.getCustomerId() != null && !parcel.getCustomerId().equals(currentUser.getId())) {
            User owner = userRepository.findById(parcel.getCustomerId()).orElse(null);
            if (owner != null) {
                senderName = owner.getFullName();
                senderPhone = owner.getPhoneNumber();
                senderEmail = owner.getEmail();
            }
        }

        // Retrieve status milestones
        List<StatusHistory> historyRecords = statusHistoryRepository.findByParcelIdOrderByChangedAtAsc(parcel.getId());
        List<ReceiptDataDto.StatusTimelineEntry> timelineEntries = new ArrayList<>();
        if (historyRecords != null) {
            for (StatusHistory record : historyRecords) {
                String formattedDate = record.getChangedAt() != null
                        ? LocalDateTime.ofInstant(record.getChangedAt(), ZoneId.systemDefault()).format(FORMATTER)
                        : "-";
                timelineEntries.add(ReceiptDataDto.StatusTimelineEntry.builder()
                        .status(record.getNewStatus() != null ? record.getNewStatus().name() : "-")
                        .changedAt(formattedDate)
                        .changedByRole(record.getChangedByRole() != null ? record.getChangedByRole() : "-")
                        .note(record.getNote())
                        .build());
            }
        }

        ReceiptDataDto receiptDto = ReceiptDataDto.builder()
                .receiptId("RCPT-" + parcel.getTrackingNumber())
                .trackingNumber(parcel.getTrackingNumber())
                .issueDate(LocalDateTime.now().format(FORMATTER))
                .senderName(senderName != null ? senderName : "Valued Customer")
                .senderEmail(senderEmail)
                .senderPhone(senderPhone)
                .pickupAddress(formatAddress(parcel.getPickup()))
                .receiverName(parcel.getReceiverName() != null ? parcel.getReceiverName() : "Recipient")
                .receiverPhone(parcel.getReceiverPhone())
                .dropoffAddress(formatAddress(parcel.getDropoff()))
                .weightKg(parcel.getWeightKg())
                .size(parcel.getSize() != null ? parcel.getSize().name() : "STANDARD")
                .currentStatus(parcel.getStatus() != null ? parcel.getStatus().name() : "CREATED")
                .serviceType("Standard Courier Delivery")
                .deliveryFee(parcel.getFee())
                .statusHistory(timelineEntries)
                .build();

        byte[] pdfBytes = receiptService.generateDeliveryReceipt(receiptDto);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"receipt-" + parcel.getTrackingNumber() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    private Parcel findParcelByIdOrTrackingNumber(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new NotFoundException("Invalid parcel identifier");
        }

        // Try numeric ID lookup first
        try {
            Long numericId = Long.parseLong(identifier.trim());
            return parcelRepository.findById(numericId)
                    .orElseGet(() -> parcelRepository.findByTrackingNumber(identifier.trim())
                            .orElseThrow(() -> new NotFoundException("Parcel not found with id: " + identifier)));
        } catch (NumberFormatException e) {
            // Not a number, lookup by tracking number
            return parcelRepository.findByTrackingNumber(identifier.trim())
                    .orElseThrow(() -> new NotFoundException("Parcel not found with tracking number: " + identifier));
        }
    }

    private String formatAddress(Address address) {
        if (address == null) {
            return "N/A";
        }
        StringBuilder sb = new StringBuilder();
        if (address.getLine1() != null && !address.getLine1().isBlank()) {
            sb.append(address.getLine1());
        }
        if (address.getCity() != null && !address.getCity().isBlank()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(address.getCity());
        }
        if (address.getDistrict() != null && !address.getDistrict().isBlank()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(address.getDistrict());
        }
        return sb.isEmpty() ? "N/A" : sb.toString();
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication != null ? authentication.getName() : null;
        if (email == null) {
            throw new ForbiddenException("Unauthenticated request");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User profile not found for email: " + email));
    }
}
