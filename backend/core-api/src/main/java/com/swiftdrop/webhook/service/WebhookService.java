package com.swiftdrop.webhook.service;

import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.merchant.domain.Merchant;
import com.swiftdrop.merchant.repository.MerchantRepository;
import com.swiftdrop.webhook.domain.WebhookDelivery;
import com.swiftdrop.webhook.domain.WebhookEndpoint;
import com.swiftdrop.webhook.dto.TestPingResponse;
import com.swiftdrop.webhook.dto.UpdateWebhookEndpointRequest;
import com.swiftdrop.webhook.dto.WebhookDeliveryDto;
import com.swiftdrop.webhook.dto.WebhookEndpointDto;
import com.swiftdrop.webhook.repository.WebhookDeliveryRepository;
import com.swiftdrop.webhook.repository.WebhookEndpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookEndpointRepository endpointRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final MerchantRepository merchantRepository;
    private final WebhookSigner webhookSigner;
    private final WebhookDispatcherService dispatcherService;

    @Transactional(readOnly = true)
    public WebhookEndpointDto getEndpoint(Long merchantId) {
        WebhookEndpoint endpoint = endpointRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new NotFoundException("No webhook endpoint registered for merchant ID: " + merchantId));
        return dispatcherService.toEndpointDto(endpoint);
    }

    @Transactional
    public WebhookEndpointDto updateOrCreateEndpoint(Long merchantId, UpdateWebhookEndpointRequest request) {
        WebhookEndpoint endpoint = endpointRepository.findByMerchantId(merchantId)
                .orElse(null);

        if (endpoint == null) {
            log.info("Creating new webhook endpoint for merchantId: {}", merchantId);
            endpoint = WebhookEndpoint.builder()
                    .merchantId(merchantId)
                    .url(request.getUrl())
                    .secret(webhookSigner.generateSecret())
                    .active(request.getActive())
                    .build();
        } else {
            log.info("Updating existing webhook endpoint ID {} for merchantId: {}", endpoint.getId(), merchantId);
            endpoint.setUrl(request.getUrl());
            endpoint.setActive(request.getActive());
        }

        endpoint = endpointRepository.save(endpoint);
        return dispatcherService.toEndpointDto(endpoint);
    }

    @Transactional
    public String regenerateSecret(Long merchantId) {
        WebhookEndpoint endpoint = endpointRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new NotFoundException("No webhook endpoint registered for merchant ID: " + merchantId));

        String newSecret = webhookSigner.generateSecret();
        endpoint.setSecret(newSecret);
        endpointRepository.save(endpoint);
        log.info("Regenerated webhook signing secret for endpoint ID: {}", endpoint.getId());
        return newSecret;
    }

    @Transactional(readOnly = true)
    public Page<WebhookDeliveryDto> getDeliveries(Long merchantId, Pageable pageable) {
        WebhookEndpoint endpoint = endpointRepository.findByMerchantId(merchantId).orElse(null);
        if (endpoint == null) {
            return Page.empty(pageable);
        }
        Page<WebhookDelivery> deliveries = deliveryRepository.findByEndpointIdOrderByCreatedAtDesc(endpoint.getId(), pageable);
        return deliveries.map(dispatcherService::toDto);
    }

    public TestPingResponse sendTestPing(Long merchantId) {
        return dispatcherService.sendTestPing(merchantId);
    }

    public WebhookDeliveryDto resendDelivery(Long deliveryId) {
        return dispatcherService.resendDelivery(deliveryId);
    }

    /**
     * Resolves current merchant ID from the authenticated user principal.
     * Falls back gracefully to first merchant or default ID (1L) for development/demo convenience.
     */
    public Long resolveCurrentMerchantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String principalName = auth.getName();
            try {
                // If principal is numeric user ID
                Long userId = Long.parseLong(principalName);
                return merchantRepository.findByUserId(userId)
                        .map(Merchant::getId)
                        .orElse(1L);
            } catch (NumberFormatException ignored) {
                // If principal is an email or username
                return merchantRepository.findAll().stream()
                        .findFirst()
                        .map(Merchant::getId)
                        .orElse(1L);
            }
        }

        // Demo fallback for local development testing
        return merchantRepository.findAll().stream()
                .findFirst()
                .map(Merchant::getId)
                .orElse(1L);
    }
}
