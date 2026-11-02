package com.swiftdrop.courier.api;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.ForbiddenException;
import com.swiftdrop.courier.dto.*;
import com.swiftdrop.courier.service.CourierService;
import com.swiftdrop.dispatch.domain.CourierProfile;
import com.swiftdrop.dispatch.repository.CourierProfileRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courier")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COURIER')")
public class CourierController {

    private final CourierService courierService;
    private final UserRepository userRepository;
    private final CourierProfileRepository courierProfileRepository;

    @GetMapping("/deliveries/today")
    public ResponseEntity<List<CourierDeliveryResponse>> getTodayDeliveries(Authentication authentication) {
        Long courierProfileId = resolveCourierProfileId(authentication);
        return ResponseEntity.ok(courierService.getTodayDeliveries(courierProfileId));
    }

    @PatchMapping("/deliveries/{parcelId}/status")
    public ResponseEntity<CourierDeliveryResponse> updateStatus(
            @PathVariable Long parcelId,
            @Valid @RequestBody UpdateStatusRequest request,
            Authentication authentication) {
        Long courierProfileId = resolveCourierProfileId(authentication);
        return ResponseEntity.ok(courierService.updateStatus(courierProfileId, parcelId, request));
    }

    @PostMapping(value = "/deliveries/{parcelId}/deliver", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CourierDeliveryResponse> markDelivered(
            @PathVariable Long parcelId,
            @Valid @RequestPart("data") DeliverParcelRequest request,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            Authentication authentication) {
        Long courierProfileId = resolveCourierProfileId(authentication);
        return ResponseEntity.ok(courierService.deliverParcel(courierProfileId, parcelId, request, photo));
    }

    @PostMapping("/deliveries/{parcelId}/fail")
    public ResponseEntity<CourierDeliveryResponse> markFailed(
            @PathVariable Long parcelId,
            @Valid @RequestBody FailParcelRequest request,
            Authentication authentication) {
        Long courierProfileId = resolveCourierProfileId(authentication);
        return ResponseEntity.ok(courierService.failParcel(courierProfileId, parcelId, request));
    }

    /**
     * Resolves the logged-in user's email from Authentication, looks up the User entity,
     * and maps it to their CourierProfile ID. Throws 403 Forbidden if not found (no fallback).
     */
    private Long resolveCourierProfileId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ForbiddenException("Not allowed: Missing authentication");
        }

        Long userId = userRepository.findByEmail(authentication.getName())
                .map(User::getId)
                .orElseThrow(() -> new ForbiddenException("Not allowed: User not found"));

        return courierProfileRepository.findByUserId(userId)
                .map(CourierProfile::getId)
                .orElseThrow(() -> new ForbiddenException("Not allowed: Courier profile not found for user"));
    }
}