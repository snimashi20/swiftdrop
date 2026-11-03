package com.swiftdrop.merchant.api;

import com.swiftdrop.common.exception.BadRequestException;
import com.swiftdrop.merchant.domain.MerchantStatus;
import com.swiftdrop.merchant.dto.MerchantResponseDto;
import com.swiftdrop.merchant.dto.RejectMerchantRequest;
import com.swiftdrop.merchant.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/merchants")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Merchant administration")
@Validated
public class MerchantAdminController {

    private final MerchantService merchantService;

    @GetMapping
    @Operation(summary = "List merchants for review")
    public Page<MerchantResponseDto> list(
            @RequestParam(required = false) MerchantStatus status,
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
        return merchantService.listForAdmin(status, pageable);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a pending merchant")
    public ResponseEntity<MerchantResponseDto> approve(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(merchantService.approve(id, authentication));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a pending merchant")
    public ResponseEntity<MerchantResponseDto> reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectMerchantRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(merchantService.reject(id, request.reason(), authentication));
    }
}
