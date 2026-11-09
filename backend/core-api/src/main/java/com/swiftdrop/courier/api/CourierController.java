package com.swiftdrop.courier.api;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.ForbiddenException;
import com.swiftdrop.courier.dto.*;
import com.swiftdrop.courier.service.CourierService;
import com.swiftdrop.dispatch.domain.CourierProfile;
import com.swiftdrop.dispatch.repository.CourierProfileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Courier Deliveries", description = "Endpoints for courier mobile delivery workflow, status transitions, and proof of delivery")
@SecurityRequirement(name = "bearerAuth")
public class CourierController {

    private final CourierService courierService;
    private final UserRepository userRepository;
    private final CourierProfileRepository courierProfileRepository;

    @GetMapping("/deliveries/today")
    @Operation(summary = "Get today's assigned deliveries", description = "Retrieves active or completed deliveries assigned to the authenticated courier for today.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deliveries retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Authenticated user has no courier profile")
    })
    public ResponseEntity<List<CourierDeliveryResponse>> getTodayDeliveries(Authentication authentication) {
        Long courierProfileId = resolveCourierProfileId(authentication);
        return ResponseEntity.ok(courierService.getTodayDeliveries(courierProfileId));
    }

    @PatchMapping("/deliveries/{parcelId}/status")
    @Operation(summary = "Update parcel delivery status", description = "Transitions parcel status along the lifecycle (PICKED_UP -> IN_TRANSIT -> OUT_FOR_DELIVERY). Enforces IDOR check.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "403", description = "Forbidden: You are not assigned to this parcel"),
            @ApiResponse(responseCode = "404", description = "Parcel not found")
    })
    public ResponseEntity<CourierDeliveryResponse> updateStatus(
            @PathVariable Long parcelId,
            @Valid @RequestBody UpdateStatusRequest request,
            Authentication authentication) {
        Long courierProfileId = resolveCourierProfileId(authentication);
        return ResponseEntity.ok(courierService.updateStatus(courierProfileId, parcelId, request));
    }

    @PostMapping(value = "/deliveries/{parcelId}/deliver", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Mark parcel delivered with proof", description = "Completes delivery by capturing recipient name, optional remarks, and proof-of-delivery photo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parcel marked DELIVERED and proof saved"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "403", description = "Forbidden: You are not assigned to this parcel"),
            @ApiResponse(responseCode = "404", description = "Parcel not found")
    })
    public ResponseEntity<CourierDeliveryResponse> markDelivered(
            @PathVariable Long parcelId,
            @Valid @RequestPart("data") DeliverParcelRequest request,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            Authentication authentication) {
        Long courierProfileId = resolveCourierProfileId(authentication);
        return ResponseEntity.ok(courierService.deliverParcel(courierProfileId, parcelId, request, photo));
    }

    @PostMapping("/deliveries/{parcelId}/fail")
    @Operation(summary = "Record delivery failure", description = "Marks delivery as FAILED with a structured failure reason and explanation note.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delivery marked as FAILED"),
            @ApiResponse(responseCode = "400", description = "Invalid failure reason"),
            @ApiResponse(responseCode = "403", description = "Forbidden: You are not assigned to this parcel"),
            @ApiResponse(responseCode = "404", description = "Parcel not found")
    })
    public ResponseEntity<CourierDeliveryResponse> markFailed(
            @PathVariable Long parcelId,
            @Valid @RequestBody FailParcelRequest request,
            Authentication authentication) {
        Long courierProfileId = resolveCourierProfileId(authentication);
        return ResponseEntity.ok(courierService.failParcel(courierProfileId, parcelId, request));
    }

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
