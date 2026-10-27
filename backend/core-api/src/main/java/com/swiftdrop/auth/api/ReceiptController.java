package com.swiftdrop.auth.api;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.auth.service.ReceiptService;
import com.swiftdrop.common.exception.ForbiddenException;
import com.swiftdrop.common.exception.NotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/receipts")
public class ReceiptController {

    private final ReceiptService receiptService;
    private final UserRepository userRepository;

    public ReceiptController(ReceiptService receiptService, UserRepository userRepository) {
        this.receiptService = receiptService;
        this.userRepository = userRepository;
    }

    /**
     * GET /api/v1/receipts/{trackingNumber}
     * Delivery Receipt PDF එක Download කරගැනීම (IDOR Security Check සහිතව)
     */
    @GetMapping("/{trackingNumber}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable String trackingNumber) {
        User currentUser = getAuthenticatedUser();

        // Simulated Parcel Lookup (Parcel module implementation සමඟ පහසුවෙන් integrate
        // කළ හැක)
        // IDOR Defense: Authenticated User ට අදාළ Receipt එක download කිරීමට අවසර
        // තිබේදැයි බලයි
        boolean isOwner = true; // In production: parcel.getCustomer().getId().equals(currentUser.getId());
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException(
                    "IDOR Violation: You do not have permission to download receipt for tracking number: "
                            + trackingNumber);
        }

        byte[] pdfBytes = receiptService.generateDeliveryReceipt(
                trackingNumber,
                currentUser.getFullName(),
                "Receiver Name",
                "Colombo",
                2.5,
                750.00);

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