package com.swiftdrop.merchant.api;

import com.swiftdrop.merchant.dto.MerchantResponseDto;
import com.swiftdrop.merchant.dto.RegisterMerchantRequest;
import com.swiftdrop.merchant.service.MerchantService;
import com.swiftdrop.merchant.service.MerchantOrderService;
import com.swiftdrop.merchant.security.CurrentMerchantProvider;
import com.swiftdrop.merchant.dto.MerchantOrderResponse;
import com.swiftdrop.parcel.domain.ParcelStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
@Validated
public class MerchantController {

    private final MerchantService merchantService;
    private final MerchantOrderService merchantOrderService;
    private final CurrentMerchantProvider currentMerchantProvider;

    @PostMapping("/register")
    public ResponseEntity<MerchantResponseDto> register(@Valid @RequestBody RegisterMerchantRequest request) {
        MerchantResponseDto created = merchantService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<MerchantResponseDto> getMyProfile(Authentication authentication) {
        MerchantResponseDto profile = merchantService.getMyProfile(authentication);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/me/orders")
    @PreAuthorize("hasRole('MERCHANT')")
    public Page<MerchantOrderResponse> getMyOrders(
            @RequestParam(required = false) ParcelStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Authentication authentication) {
        if (page < 0 || size < 1 || size > 100) {
            throw new com.swiftdrop.common.exception.BadRequestException("Invalid page or size");
        }
        Long merchantId = currentMerchantProvider.getCurrentMerchant(authentication).getId();
        return merchantOrderService.listDashboard(merchantId, status, PageRequest.of(page, size));
    }

    @GetMapping("/me/orders/summary")
    @PreAuthorize("hasRole('MERCHANT')")
    public java.util.Map<ParcelStatus, Long> getMyOrderSummary(Authentication authentication) {
        Long merchantId = currentMerchantProvider.getCurrentMerchant(authentication).getId();
        return merchantOrderService.countDashboardByStatus(merchantId);
    }
}
