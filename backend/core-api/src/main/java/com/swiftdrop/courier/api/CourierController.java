package com.swiftdrop.courier.api;

import com.swiftdrop.courier.dto.*;
import com.swiftdrop.courier.service.CourierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/courier")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COURIER')")
public class CourierController {

    private final CourierService courierService;

    @GetMapping("/deliveries/today")
    public ResponseEntity<List<CourierDeliveryResponse>> getTodayDeliveries(
            @AuthenticationPrincipal Principal principal) {
        Long courierId = extractUserId(principal);
        return ResponseEntity.ok(courierService.getTodayDeliveries(courierId));
    }

    @PatchMapping("/deliveries/{parcelId}/status")
    public ResponseEntity<CourierDeliveryResponse> updateStatus(
            @PathVariable Long parcelId,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal Principal principal) {
        Long courierId = extractUserId(principal);
        return ResponseEntity.ok(courierService.updateStatus(courierId, parcelId, request));
    }

    @PostMapping(value = "/deliveries/{parcelId}/deliver", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CourierDeliveryResponse> markDelivered(
            @PathVariable Long parcelId,
            @Valid @RequestPart("data") DeliverParcelRequest request,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @AuthenticationPrincipal Principal principal) {
        Long courierId = extractUserId(principal);
        return ResponseEntity.ok(courierService.deliverParcel(courierId, parcelId, request, photo));
    }

    @PostMapping("/deliveries/{parcelId}/fail")
    public ResponseEntity<CourierDeliveryResponse> markFailed(
            @PathVariable Long parcelId,
            @Valid @RequestBody FailParcelRequest request,
            @AuthenticationPrincipal Principal principal) {
        Long courierId = extractUserId(principal);
        return ResponseEntity.ok(courierService.failParcel(courierId, parcelId, request));
    }

    private Long extractUserId(Principal principal) {
        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            return 1L;
        }
    }
}