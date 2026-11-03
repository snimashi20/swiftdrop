package com.swiftdrop.merchant.api;

import com.swiftdrop.merchant.dto.ApiKeyGeneratedResponse;
import com.swiftdrop.merchant.dto.ApiKeyResponse;
import com.swiftdrop.merchant.dto.CreateApiKeyRequest;
import com.swiftdrop.merchant.security.CurrentMerchantProvider;
import com.swiftdrop.merchant.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/merchants/me/api-keys")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MERCHANT')")
@Tag(name = "Merchant API keys")
public class MerchantApiKeyController {

    private final ApiKeyService apiKeyService;
    private final CurrentMerchantProvider currentMerchantProvider;

    @PostMapping
    @Operation(summary = "Generate a merchant API key")
    public ResponseEntity<ApiKeyGeneratedResponse> generate(
            @Valid @RequestBody CreateApiKeyRequest request,
            Authentication authentication) {
        Long merchantId = currentMerchantProvider.getCurrentMerchant(authentication).getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiKeyService.generate(merchantId, request.label()));
    }

    @GetMapping
    @Operation(summary = "List merchant API keys")
    public List<ApiKeyResponse> list(Authentication authentication) {
        Long merchantId = currentMerchantProvider.getCurrentMerchant(authentication).getId();
        return apiKeyService.list(merchantId);
    }

    @PostMapping("/{id}/revoke")
    @Operation(summary = "Revoke a merchant API key")
    public ApiKeyResponse revoke(@PathVariable Long id, Authentication authentication) {
        Long merchantId = currentMerchantProvider.getCurrentMerchant(authentication).getId();
        return apiKeyService.revoke(merchantId, id);
    }
}
