package com.swiftdrop.webhook.service;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.ForbiddenException;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.merchant.domain.Merchant;
import com.swiftdrop.merchant.repository.MerchantRepository;
import com.swiftdrop.merchant.security.MerchantPrincipal;
import com.swiftdrop.webhook.domain.DeliveryStatus;
import com.swiftdrop.webhook.domain.WebhookDelivery;
import com.swiftdrop.webhook.domain.WebhookEndpoint;
import com.swiftdrop.webhook.dto.WebhookDeliveryDto;
import com.swiftdrop.webhook.dto.WebhookEndpointDto;
import com.swiftdrop.webhook.repository.WebhookDeliveryRepository;
import com.swiftdrop.webhook.repository.WebhookEndpointRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private WebhookEndpointRepository endpointRepository;

    @Mock
    private WebhookDeliveryRepository deliveryRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WebhookSigner webhookSigner;

    @Mock
    private WebhookDispatcherService dispatcherService;

    @InjectMocks
    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("resolveCurrentMerchantId should resolve merchant via email when principal is username/email")
    void shouldResolveMerchantViaEmail() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("merchant@shop.com", "credentials");
        SecurityContextHolder.getContext().setAuthentication(auth);

        User user = new User();
        user.setId(200L);
        user.setEmail("merchant@shop.com");

        Merchant merchant = Merchant.builder().id(55L).user(user).build();

        when(userRepository.findByEmailIgnoreCase("merchant@shop.com")).thenReturn(Optional.of(user));
        when(merchantRepository.findByUserId(200L)).thenReturn(Optional.of(merchant));

        Long resolvedId = webhookService.resolveCurrentMerchantId();

        assertThat(resolvedId).isEqualTo(55L);
    }

    @Test
    @DisplayName("resolveCurrentMerchantId should resolve directly when principal is MerchantPrincipal")
    void shouldResolveFromMerchantPrincipal() {
        MerchantPrincipal principal = new MerchantPrincipal(77L, "merchant@shop.com", "ROLE_MERCHANT");
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, "credentials");
        SecurityContextHolder.getContext().setAuthentication(auth);

        Long resolvedId = webhookService.resolveCurrentMerchantId();

        assertThat(resolvedId).isEqualTo(77L);
    }

    @Test
    @DisplayName("resolveCurrentMerchantId should throw ForbiddenException when unauthenticated or anonymous")
    void shouldThrowForbiddenWhenUnauthenticated() {
        assertThatThrownBy(() -> webhookService.resolveCurrentMerchantId())
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Authentication required");
    }

    @Test
    @DisplayName("resolveCurrentMerchantId should throw ForbiddenException when merchant profile is missing")
    void shouldThrowForbiddenWhenMerchantNotFound() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("unknown@shop.com", "credentials");
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findByEmailIgnoreCase("unknown@shop.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> webhookService.resolveCurrentMerchantId())
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("No merchant profile found");
    }

    @Test
    @DisplayName("resendDelivery should reject cross-tenant resend attempt (IDOR protection) with 403")
    void shouldPreventIdorOnResendDelivery() {
        WebhookEndpoint ownerEndpoint = WebhookEndpoint.builder().id(1L).merchantId(100L).build();
        WebhookDelivery delivery = WebhookDelivery.builder()
                .id(42L)
                .endpoint(ownerEndpoint)
                .status(DeliveryStatus.FAILED)
                .build();

        when(deliveryRepository.findById(42L)).thenReturn(Optional.of(delivery));

        // Attacker is merchant 999 attempting to resend merchant 100's delivery
        assertThatThrownBy(() -> webhookService.resendDelivery(42L, 999L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You are not authorized to resend this webhook delivery");

        verify(dispatcherService, never()).resendDelivery(any(WebhookDelivery.class));
    }

    @Test
    @DisplayName("resendDelivery should allow resend when calling merchant owns the delivery endpoint")
    void shouldAllowResendForOwnerMerchant() {
        WebhookEndpoint ownerEndpoint = WebhookEndpoint.builder().id(1L).merchantId(100L).build();
        WebhookDelivery delivery = WebhookDelivery.builder()
                .id(42L)
                .endpoint(ownerEndpoint)
                .status(DeliveryStatus.FAILED)
                .build();

        WebhookDeliveryDto expectedDto = WebhookDeliveryDto.builder()
                .id(42L)
                .status(DeliveryStatus.SUCCESS)
                .attempts(1)
                .build();

        when(deliveryRepository.findById(42L)).thenReturn(Optional.of(delivery));
        when(dispatcherService.resendDelivery(delivery)).thenReturn(expectedDto);

        WebhookDeliveryDto result = webhookService.resendDelivery(42L, 100L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(42L);
        verify(dispatcherService).resendDelivery(delivery);
    }

    @Test
    @DisplayName("getEndpoint should return masked secret for security")
    void shouldReturnMaskedSecretInGetEndpoint() {
        WebhookEndpoint endpoint = WebhookEndpoint.builder()
                .id(1L)
                .merchantId(100L)
                .url("https://merchant.com/webhook")
                .secret("whsec_1234567890abcdef123456")
                .active(true)
                .createdAt(Instant.now())
                .build();

        WebhookEndpointDto maskedDto = WebhookEndpointDto.builder()
                .id(1L)
                .merchantId(100L)
                .url("https://merchant.com/webhook")
                .secret("whsec_******************3456")
                .active(true)
                .build();

        when(endpointRepository.findByMerchantId(100L)).thenReturn(Optional.of(endpoint));
        when(dispatcherService.toEndpointDto(endpoint, false)).thenReturn(maskedDto);

        WebhookEndpointDto result = webhookService.getEndpoint(100L);

        assertThat(result.getSecret()).contains("*");
        assertThat(result.getSecret()).endsWith("3456");
    }
}
