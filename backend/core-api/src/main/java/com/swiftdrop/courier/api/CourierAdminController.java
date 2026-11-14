package com.swiftdrop.courier.api;

import com.swiftdrop.common.exception.BadRequestException;
import com.swiftdrop.courier.domain.CourierApplicationStatus;
import com.swiftdrop.courier.dto.CourierApplicationResponseDto;
import com.swiftdrop.courier.dto.RejectCourierRequest;
import com.swiftdrop.courier.service.CourierApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/couriers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Courier administration")
@Validated
public class CourierAdminController {

    private final CourierApplicationService courierApplicationService;

    @GetMapping
    @Operation(summary = "List courier applications for review")
    public Page<CourierApplicationResponseDto> list(
            @RequestParam(required = false) CourierApplicationStatus status,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be greater than or equal to 0")
            int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Page size must be at least 1")
            @Max(value = 100, message = "Page size must not exceed 100")
            int size) {
        if (page < 0) {
            throw new BadRequestException("Page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("Page size must be between 1 and 100");
        }
        Pageable pageable = PageRequest.of(page, size);
        return courierApplicationService.listForAdmin(status, pageable);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a pending courier application")
    public ResponseEntity<CourierApplicationResponseDto> approve(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(courierApplicationService.approve(id, authentication));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a pending courier application")
    public ResponseEntity<CourierApplicationResponseDto> reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectCourierRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(courierApplicationService.reject(id, request.reason(), authentication));
    }
}
