package com.swiftdrop.webhook.service;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.ForbiddenException;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.merchant.domain.Merchant;
import com.swiftdrop.merchant.repository.MerchantRepository;
import com.swiftdrop.merchant.security.MerchantPrincipal;
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
    private final UserRepository userRepository;
    private final WebhookSigner webhookSigner;
    private final WebhookDispatcherService dispatcherService;

    @Transactional(readOnly = true)
    public WebhookEndpointDto getEndpoint(Long merchantId) {
        WebhookEndpoint endpoint = endpointRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new NotFoundException("No webhook endpoint registered for merchant ID: " + merchantId));
        return dispatcherService.toEndpointDto(endpoint, false); // Masked secret
    }

    @Transactional
    public WebhookEndpointDto updateOrCreateEndpoint(Long merchantId, UpdateWebhookEndpointRequest request) {
        WebhookEndpoint endpoint = endpointRepository.findByMerchantId(merchantId)
                .orElse(null);

        boolean isNew = (endpoint == null);
        if (isNew) {
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
        // Expose full secret only on initial endpoint creation so merchant can store it securely
        return dispatcherService.toEndpointDto(endpoint, isNew);
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
        Long currentMerchantId = resolveCurrentMerchantId();
        return resendDelivery(deliveryId, currentMerchantId);
    }

    public WebhookDeliveryDto resendDelivery(Long deliveryId, Long merchantId) {
        WebhookDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NotFoundException("Webhook delivery not found with ID: " + deliveryId));

        if (delivery.getEndpoint() == null || !delivery.getEndpoint().getMerchantId().equals(merchantId)) {
            log.warn("IDOR attempt: Merchant {} attempted to resend delivery {} belonging to endpoint merchant {}",
                    merchantId, deliveryId, delivery.getEndpoint() != null ? delivery.getEndpoint().getMerchantId() : null);
            throw new ForbiddenException("You are not authorized to resend this webhook delivery");
        }

        return dispatcherService.resendDelivery(delivery);
    }

    /**
     * Resolves current merchant ID from the authenticated user principal.
     * Looks up user by email/username or numeric ID and throws 403 Forbidden if not authenticated
     * or no associated merchant profile exists.
     */
    public Long resolveCurrentMerchantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ForbiddenException("Authentication required");
        }

        if (auth.getPrincipal() instanceof MerchantPrincipal principal) {
            return principal.merchantId();
        }

        String principalName = auth.getName();
        try {
            Long userId = Long.parseLong(principalName);
            return merchantRepository.findByUserId(userId)
                    .map(Merchant::getId)
                    .orElseThrow(() -> new ForbiddenException("No merchant profile associated with user ID: " + userId));
        } catch (NumberFormatException ignored) {
            // Principal is an email or username
            return userRepository.findByEmailIgnoreCase(principalName)
                    .flatMap(user -> merchantRepository.findByUserId(user.getId()))
                    .map(Merchant::getId)
                    .orElseThrow(() -> new ForbiddenException("No merchant profile found for user: " + principalName));
        }
    }
}
