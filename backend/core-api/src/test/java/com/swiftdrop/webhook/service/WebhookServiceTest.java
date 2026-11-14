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
import com.swiftdrop.webhook.dto.TestPingResponse;
import com.swiftdrop.webhook.dto.UpdateWebhookEndpointRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
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
                new UsernamePasswordAuthenticationToken("merchant@shop.com", "credentials", List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        User user = new User();
        user.setId(200L);
        user.setEmail("merchant@shop.com");

        Merchant merchant = Merchant.builder().id(55L).userId(user.getId()).build();

        when(userRepository.findByEmailIgnoreCase("merchant@shop.com")).thenReturn(Optional.of(user));
        when(merchantRepository.findByUserId(200L)).thenReturn(Optional.of(merchant));

        Long resolvedId = webhookService.resolveCurrentMerchantId();

        assertThat(resolvedId).isEqualTo(55L);
    }

    @Test
    @DisplayName("resolveCurrentMerchantId should resolve directly when principal is MerchantPrincipal")
    void shouldResolveFromMerchantPrincipal() {
        MerchantPrincipal principal = new MerchantPrincipal(77L);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, "credentials", List.of());
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
                new UsernamePasswordAuthenticationToken("unknown@shop.com", "credentials", List.of());
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

    @Test
    @DisplayName("getEndpoint should throw NotFoundException when endpoint does not exist")
    void shouldThrowNotFoundInGetEndpointWhenMissing() {
        when(endpointRepository.findByMerchantId(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> webhookService.getEndpoint(404L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("No webhook endpoint registered for merchant ID: 404");
    }

    @Test
    @DisplayName("updateOrCreateEndpoint should create new endpoint with generated secret when none exists")
    void shouldCreateNewEndpointWhenNoneExists() {
        UpdateWebhookEndpointRequest req = UpdateWebhookEndpointRequest.builder()
                .url("https://merchant.com/webhook")
                .active(true)
                .build();

        when(endpointRepository.findByMerchantId(100L)).thenReturn(Optional.empty());
        when(webhookSigner.generateSecret()).thenReturn("whsec_new_generated_secret_123");
        when(endpointRepository.save(any(WebhookEndpoint.class))).thenAnswer(inv -> {
            WebhookEndpoint ep = inv.getArgument(0);
            ep.setId(10L);
            return ep;
        });

        WebhookEndpointDto expectedDto = WebhookEndpointDto.builder()
                .id(10L)
                .merchantId(100L)
                .url("https://merchant.com/webhook")
                .secret("whsec_new_generated_secret_123")
                .active(true)
                .build();
        when(dispatcherService.toEndpointDto(any(WebhookEndpoint.class), eq(true))).thenReturn(expectedDto);

        WebhookEndpointDto result = webhookService.updateOrCreateEndpoint(100L, req);

        assertThat(result).isNotNull();
        assertThat(result.getSecret()).isEqualTo("whsec_new_generated_secret_123");
        verify(webhookSigner).generateSecret();
        verify(endpointRepository).save(any(WebhookEndpoint.class));
    }

    @Test
    @DisplayName("updateOrCreateEndpoint should update existing endpoint without changing secret")
    void shouldUpdateExistingEndpoint() {
        WebhookEndpoint existing = WebhookEndpoint.builder()
                .id(5L)
                .merchantId(100L)
                .url("https://old.com/webhook")
                .secret("whsec_existing_secret")
                .active(true)
                .build();

        UpdateWebhookEndpointRequest req = UpdateWebhookEndpointRequest.builder()
                .url("https://new.com/webhook")
                .active(false)
                .build();

        when(endpointRepository.findByMerchantId(100L)).thenReturn(Optional.of(existing));
        when(endpointRepository.save(existing)).thenReturn(existing);

        WebhookEndpointDto updatedDto = WebhookEndpointDto.builder()
                .id(5L)
                .merchantId(100L)
                .url("https://new.com/webhook")
                .secret("whsec_****cret")
                .active(false)
                .build();
        when(dispatcherService.toEndpointDto(existing, false)).thenReturn(updatedDto);

        WebhookEndpointDto result = webhookService.updateOrCreateEndpoint(100L, req);

        assertThat(result).isNotNull();
        assertThat(existing.getUrl()).isEqualTo("https://new.com/webhook");
        assertThat(existing.getActive()).isFalse();
        verify(webhookSigner, never()).generateSecret();
    }

    @Test
    @DisplayName("regenerateSecret should generate and save new secret when endpoint exists")
    void shouldRegenerateSecretSuccessfully() {
        WebhookEndpoint endpoint = WebhookEndpoint.builder()
                .id(1L)
                .merchantId(100L)
                .secret("whsec_old")
                .build();

        when(endpointRepository.findByMerchantId(100L)).thenReturn(Optional.of(endpoint));
        when(webhookSigner.generateSecret()).thenReturn("whsec_brand_new_secret");

        String newSecret = webhookService.regenerateSecret(100L);

        assertThat(newSecret).isEqualTo("whsec_brand_new_secret");
        assertThat(endpoint.getSecret()).isEqualTo("whsec_brand_new_secret");
        verify(endpointRepository).save(endpoint);
    }

    @Test
    @DisplayName("regenerateSecret should throw NotFoundException when endpoint does not exist")
    void shouldThrowNotFoundOnRegenerateSecretWhenMissing() {
        when(endpointRepository.findByMerchantId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> webhookService.regenerateSecret(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("No webhook endpoint registered for merchant ID: 999");
    }

    @Test
    @DisplayName("getDeliveries should return empty page when no endpoint registered")
    void shouldReturnEmptyPageWhenNoEndpoint() {
        Pageable pageable = PageRequest.of(0, 10);
        when(endpointRepository.findByMerchantId(100L)).thenReturn(Optional.empty());

        Page<WebhookDeliveryDto> result = webhookService.getDeliveries(100L, pageable);

        assertThat(result).isEmpty();
        verify(deliveryRepository, never()).findByEndpointIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    @DisplayName("getDeliveries should return mapped page when deliveries exist")
    void shouldReturnMappedDeliveriesPage() {
        Pageable pageable = PageRequest.of(0, 10);
        WebhookEndpoint endpoint = WebhookEndpoint.builder().id(1L).merchantId(100L).build();
        WebhookDelivery delivery = WebhookDelivery.builder().id(10L).endpoint(endpoint).build();
        WebhookDeliveryDto dto = WebhookDeliveryDto.builder().id(10L).build();

        when(endpointRepository.findByMerchantId(100L)).thenReturn(Optional.of(endpoint));
        when(deliveryRepository.findByEndpointIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(delivery)));
        when(dispatcherService.toDto(delivery)).thenReturn(dto);

        Page<WebhookDeliveryDto> result = webhookService.getDeliveries(100L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("sendTestPing should delegate to dispatcherService")
    void shouldDelegateTestPing() {
        TestPingResponse expectedResponse = TestPingResponse.builder().success(true).httpStatus(200).build();
        when(dispatcherService.sendTestPing(100L)).thenReturn(expectedResponse);

        TestPingResponse actual = webhookService.sendTestPing(100L);

        assertThat(actual.isSuccess()).isTrue();
        assertThat(actual.getHttpStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("resendDelivery(Long deliveryId) should resolve merchant and call resend")
    void shouldResolveMerchantAndResend() {
        MerchantPrincipal principal = new MerchantPrincipal(55L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "cred", List.of()));

        WebhookEndpoint endpoint = WebhookEndpoint.builder().id(1L).merchantId(55L).build();
        WebhookDelivery delivery = WebhookDelivery.builder().id(20L).endpoint(endpoint).build();
        WebhookDeliveryDto dto = WebhookDeliveryDto.builder().id(20L).status(DeliveryStatus.SUCCESS).build();

        when(deliveryRepository.findById(20L)).thenReturn(Optional.of(delivery));
        when(dispatcherService.resendDelivery(delivery)).thenReturn(dto);

        WebhookDeliveryDto result = webhookService.resendDelivery(20L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("resendDelivery should throw NotFoundException when delivery does not exist")
    void shouldThrowNotFoundWhenDeliveryMissing() {
        when(deliveryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> webhookService.resendDelivery(999L, 55L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Webhook delivery not found with ID: 999");
    }

    @Test
    @DisplayName("resendDelivery should throw ForbiddenException when delivery endpoint is null")
    void shouldThrowForbiddenWhenDeliveryEndpointIsNull() {
        WebhookDelivery delivery = WebhookDelivery.builder().id(25L).endpoint(null).build();
        when(deliveryRepository.findById(25L)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> webhookService.resendDelivery(25L, 55L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You are not authorized to resend this webhook delivery");
    }

    @Test
    @DisplayName("resolveCurrentMerchantId should resolve with numeric userId and handle missing profile")
    void shouldResolveNumericUserId() {
        // Successful numeric user ID
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("300", "cred", List.of()));

        Merchant merchant = Merchant.builder().id(88L).userId(300L).build();
        when(merchantRepository.findByUserId(300L)).thenReturn(Optional.of(merchant));

        assertThat(webhookService.resolveCurrentMerchantId()).isEqualTo(88L);

        // Numeric user ID without merchant profile
        when(merchantRepository.findByUserId(300L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> webhookService.resolveCurrentMerchantId())
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("No merchant profile associated with user ID: 300");
    }

    @Test
    @DisplayName("resolveCurrentMerchantId should throw ForbiddenException when email user has no merchant profile")
    void shouldThrowForbiddenWhenEmailUserHasNoMerchant() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user@shop.com", "cred", List.of()));

        User user = new User();
        user.setId(500L);
        user.setEmail("user@shop.com");

        when(userRepository.findByEmailIgnoreCase("user@shop.com")).thenReturn(Optional.of(user));
        when(merchantRepository.findByUserId(500L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> webhookService.resolveCurrentMerchantId())
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("No merchant profile found for user: user@shop.com");
    }
}
