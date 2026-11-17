package com.swiftdrop.courier.api;

import com.swiftdrop.courier.dto.CourierRegistrationResponse;
import com.swiftdrop.courier.dto.RegisterCourierRequest;
import com.swiftdrop.courier.service.CourierOnboardingService;
import com.swiftdrop.courier.service.FileStorageService;
import com.swiftdrop.dispatch.service.ZoneLookup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courier")
@RequiredArgsConstructor
@Tag(name = "Courier Onboarding", description = "Public courier registration, delivery zones lookup, and Admin KYC document preview")
public class CourierOnboardingController {

    private final CourierOnboardingService onboardingService;
    private final FileStorageService fileStorageService;

    @GetMapping("/zones")
    @Operation(summary = "List all operating delivery zones", description = "Returns all 25 country-wide Sri Lankan delivery zones for courier onboarding.")
    public ResponseEntity<List<ZoneLookup.ZoneOption>> listZones() {
        return ResponseEntity.ok(onboardingService.listAvailableZones());
    }

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Register a new courier applicant", description = "Creates a pending courier user account, courier profile, and stores both NIC front and back photos.")
    public ResponseEntity<CourierRegistrationResponse> register(
            @Valid @RequestPart("data") RegisterCourierRequest request,
            @RequestPart("nicFront") MultipartFile nicFront,
            @RequestPart("nicBack") MultipartFile nicBack) {
        CourierRegistrationResponse created = onboardingService.register(request, nicFront, nicBack);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/documents/{category}/{filename}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Fetch courier KYC document image", description = "Admin-only endpoint to securely view uploaded NIC front/back photos during approval review.")
    public ResponseEntity<Resource> getDocument(
            @PathVariable String category,
            @PathVariable String filename) {
        Resource resource = fileStorageService.loadDocument(category, filename);
        MediaType mediaType = filename.toLowerCase().endsWith(".png")
                ? MediaType.IMAGE_PNG
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }
}