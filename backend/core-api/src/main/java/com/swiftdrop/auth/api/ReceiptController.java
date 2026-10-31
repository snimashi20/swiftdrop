package com.swiftdrop.auth.api;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.auth.service.ReceiptService;
import com.swiftdrop.common.exception.ForbiddenException;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.parcel.domain.Parcel;
import com.swiftdrop.parcel.repository.ParcelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;
    private final UserRepository userRepository;
    private final ParcelRepository parcelRepository;

    /**
     * GET /api/v1/receipts/{trackingNumber}
     * Delivery Receipt PDF download with IDOR protection
     */
    @GetMapping("/{trackingNumber}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable String trackingNumber) {
        User currentUser = getAuthenticatedUser();

        Parcel parcel = parcelRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new NotFoundException("Parcel not found for tracking number: " + trackingNumber));

        // IDOR Defense: Only the parcel owner or an ADMIN may download the receipt
        boolean isOwner = parcel.getCustomerId() != null && parcel.getCustomerId().equals(currentUser.getId());
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException(
                    "IDOR Violation: You do not have permission to download receipt for tracking number: "
                            + trackingNumber);
        }

        String recipientName = parcel.getReceiverName() != null ? parcel.getReceiverName() : "Recipient";
        String destinationCity = parcel.getDropoff() != null && parcel.getDropoff().getCity() != null
                ? parcel.getDropoff().getCity()
                : "N/A";
        double weightKg = parcel.getWeightKg() != null ? parcel.getWeightKg().doubleValue() : 1.0;
        double price = parcel.getFee() != null ? parcel.getFee().doubleValue() : 0.0;

        byte[] pdfBytes = receiptService.generateDeliveryReceipt(
                parcel.getTrackingNumber(),
                currentUser.getFullName(),
                recipientName,
                destinationCity,
                weightKg,
                price);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt-" + trackingNumber + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User profile not found for email: " + email));
    }
}