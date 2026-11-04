package com.swiftdrop.merchant.api;

import com.swiftdrop.common.exception.IdempotencyKeyRequiredException;
import com.swiftdrop.merchant.dto.MerchantOrderRequest;
import com.swiftdrop.merchant.dto.MerchantOrderResponse;
import com.swiftdrop.merchant.security.MerchantPrincipal;
import com.swiftdrop.merchant.service.MerchantOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/merchant-api/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MERCHANT')")
@Tag(name = "Merchant orders")
public class MerchantOrderController {
    private final MerchantOrderService orderService;

    @PostMapping
    @Operation(summary = "Create a merchant order", description = "Requires X-API-Key and Idempotency-Key headers.")
    public ResponseEntity<MerchantOrderResponse> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Valid @RequestBody MerchantOrderRequest request,
            Authentication authentication) {
        requireKey(key);
        Long merchantId = ((MerchantPrincipal) authentication.getPrincipal()).merchantId();
        var result = orderService.create(merchantId, key, request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.response());
    }

    @PostMapping("/bulk")
    @Operation(summary = "Create merchant orders in bulk", description = "Requires X-API-Key and one Idempotency-Key for the batch.")
    public ResponseEntity<List<MerchantOrderResponse>> createBulk(
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @RequestBody @NotEmpty @Valid List<MerchantOrderRequest> requests,
            Authentication authentication) {
        requireKey(key);
        if (requests.size() > 50) {
            throw new com.swiftdrop.common.exception.ValidationException("Bulk orders cannot contain more than 50 items");
        }
        Long merchantId = ((MerchantPrincipal) authentication.getPrincipal()).merchantId();
        var result = orderService.createBulk(merchantId, key, requests);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(result.responses());
    }

    @GetMapping("/{trackingNumber}")
    @Operation(summary = "Get a merchant order")
    public MerchantOrderResponse get(@PathVariable String trackingNumber, Authentication authentication) {
        return orderService.get(merchantId(authentication), trackingNumber);
    }

    @GetMapping
    @Operation(summary = "List merchant orders")
    public Page<MerchantOrderResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Authentication authentication) {
        return orderService.list(merchantId(authentication), PageRequest.of(page, size));
    }

    private Long merchantId(Authentication authentication) {
        return ((MerchantPrincipal) authentication.getPrincipal()).merchantId();
    }

    private void requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IdempotencyKeyRequiredException();
        }
    }
}
