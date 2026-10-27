package com.swiftdrop.merchant.service;

import com.swiftdrop.common.exception.BadRequestException;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.merchant.domain.ApiKey;
import com.swiftdrop.merchant.domain.Merchant;
import com.swiftdrop.merchant.domain.MerchantStatus;
import com.swiftdrop.merchant.dto.ApiKeyGeneratedResponse;
import com.swiftdrop.merchant.dto.ApiKeyMapper;
import com.swiftdrop.merchant.dto.ApiKeyResponse;
import com.swiftdrop.merchant.exception.MerchantNotApprovedException;
import com.swiftdrop.merchant.repository.ApiKeyRepository;
import com.swiftdrop.merchant.repository.MerchantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private MerchantRepository merchantRepository;

    private final ApiKeyMapper apiKeyMapper = Mappers.getMapper(ApiKeyMapper.class);

    private ApiKeyService apiKeyService;

    @BeforeEach
    void setUp() {
        apiKeyService = new ApiKeyService(apiKeyRepository, merchantRepository, apiKeyMapper);
    }

    @Nested
    @DisplayName("generate")
    class GenerateTests {

        @Test
        @DisplayName("Approved merchant can generate an API key with plain key returned once and only hash stored")
        void generate_whenMerchantApproved_createsAndReturnsApiKeyWithPlainKeyOnce() {
            Merchant merchant = Merchant.builder()
                    .id(1L)
                    .businessName("Binu Retail")
                    .registrationNumber("BR-12345")
                    .status(MerchantStatus.APPROVED)
                    .build();

            when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));
            when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> {
                ApiKey toSave = invocation.getArgument(0);
                toSave.setId(10L);
                return toSave;
            });

            ApiKeyGeneratedResponse response = apiKeyService.generate(1L, "Production Key");

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.merchantId()).isEqualTo(1L);
            assertThat(response.label()).isEqualTo("Production Key");
            assertThat(response.active()).isTrue();

            // Check raw key format: sdk_ + 40 URL-safe characters = 44 total length
            String plainKey = response.apiKey();
            assertThat(plainKey).isNotNull();
            assertThat(plainKey).startsWith("sdk_");
            assertThat(plainKey).hasSize(44);

            // Check prefix: first 12 chars
            assertThat(response.keyPrefix()).isEqualTo(plainKey.substring(0, 12));

            // Verify stored entity contains ONLY the SHA-256 hash and never the raw key
            ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
            verify(apiKeyRepository).save(captor.capture());
            ApiKey saved = captor.getValue();

            assertThat(saved.getKeyPrefix()).isEqualTo(response.keyPrefix());
            assertThat(saved.getKeyHash()).hasSize(64);
            assertThat(saved.getKeyHash()).matches("^[0-9a-f]{64}$");
            assertThat(saved.getKeyHash()).isEqualTo(apiKeyService.hashKey(plainKey));
            assertThat(saved.getKeyHash()).isNotEqualTo(plainKey);
            assertThat(saved.isActive()).isTrue();
        }

        @Test
        @DisplayName("Pending merchant cannot generate API keys and throws MerchantNotApprovedException")
        void generate_whenMerchantPending_throwsMerchantNotApprovedException() {
            Merchant merchant = Merchant.builder()
                    .id(2L)
                    .businessName("Pending Shop")
                    .registrationNumber("PS-99999")
                    .status(MerchantStatus.PENDING)
                    .build();

            when(merchantRepository.findById(2L)).thenReturn(Optional.of(merchant));

            assertThatThrownBy(() -> apiKeyService.generate(2L, "Test Key"))
                    .isInstanceOf(MerchantNotApprovedException.class)
                    .hasMessageContaining("Merchant is not approved");

            verify(apiKeyRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rejected merchant cannot generate API keys and throws MerchantNotApprovedException")
        void generate_whenMerchantRejected_throwsMerchantNotApprovedException() {
            Merchant merchant = Merchant.builder()
                    .id(3L)
                    .businessName("Rejected Shop")
                    .registrationNumber("RS-88888")
                    .status(MerchantStatus.REJECTED)
                    .build();

            when(merchantRepository.findById(3L)).thenReturn(Optional.of(merchant));

            assertThatThrownBy(() -> apiKeyService.generate(3L, "Test Key"))
                    .isInstanceOf(MerchantNotApprovedException.class)
                    .hasMessageContaining("Merchant is not approved");

            verify(apiKeyRepository, never()).save(any());
        }

        @Test
        @DisplayName("Non-existent merchant throws NotFoundException")
        void generate_whenMerchantNotFound_throwsNotFoundException() {
            when(merchantRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> apiKeyService.generate(99L, "Test Key"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Merchant not found");

            verify(apiKeyRepository, never()).save(any());
        }

        @Test
        @DisplayName("Blank label throws BadRequestException")
        void generate_whenLabelBlank_throwsBadRequestException() {
            assertThatThrownBy(() -> apiKeyService.generate(1L, "   "))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("label is required");

            assertThatThrownBy(() -> apiKeyService.generate(1L, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("label is required");
        }

        @Test
        @DisplayName("Label exceeding 100 characters throws BadRequestException")
        void generate_whenLabelTooLong_throwsBadRequestException() {
            String longLabel = "a".repeat(101);
            assertThatThrownBy(() -> apiKeyService.generate(1L, longLabel))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("must not exceed 100 characters");
        }
    }

    @Nested
    @DisplayName("list")
    class ListTests {

        @Test
        @DisplayName("Lists all API keys for merchant without revealing plain keys or hashes")
        void list_returnsKeysWithoutSensitiveData() {
            Merchant merchant = Merchant.builder().id(1L).build();
            ApiKey key1 = ApiKey.builder()
                    .id(101L)
                    .merchant(merchant)
                    .label("Store Front")
                    .keyPrefix("sdk_abc12345")
                    .keyHash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                    .active(true)
                    .createdAt(Instant.now())
                    .build();

            when(merchantRepository.existsById(1L)).thenReturn(true);
            when(apiKeyRepository.findByMerchantIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(key1));

            List<ApiKeyResponse> result = apiKeyService.list(1L);

            assertThat(result).hasSize(1);
            ApiKeyResponse response = result.get(0);
            assertThat(response.id()).isEqualTo(101L);
            assertThat(response.merchantId()).isEqualTo(1L);
            assertThat(response.label()).isEqualTo("Store Front");
            assertThat(response.keyPrefix()).isEqualTo("sdk_abc12345");
            assertThat(response.active()).isTrue();
        }

        @Test
        @DisplayName("Throws NotFoundException if merchant does not exist")
        void list_whenMerchantNotFound_throwsNotFoundException() {
            when(merchantRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> apiKeyService.list(99L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Merchant not found");
        }
    }

    @Nested
    @DisplayName("revoke")
    class RevokeTests {

        @Test
        @DisplayName("Owner merchant can revoke an active API key")
        void revoke_whenOwner_deactivatesKey() {
            Merchant merchant = Merchant.builder().id(1L).build();
            ApiKey apiKey = ApiKey.builder()
                    .id(20L)
                    .merchant(merchant)
                    .label("Old Key")
                    .keyPrefix("sdk_xyz98765")
                    .keyHash("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                    .active(true)
                    .createdAt(Instant.now().minusSeconds(3600))
                    .build();

            when(apiKeyRepository.findByIdAndMerchantId(20L, 1L)).thenReturn(Optional.of(apiKey));
            when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ApiKeyResponse response = apiKeyService.revoke(1L, 20L);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(20L);
            assertThat(response.active()).isFalse();
            assertThat(response.revokedAt()).isNotNull();

            verify(apiKeyRepository).save(apiKey);
            assertThat(apiKey.isActive()).isFalse();
            assertThat(apiKey.getRevokedAt()).isNotNull();
        }

        @Test
        @DisplayName("Revoke throws NotFoundException when key does not belong to merchant")
        void revoke_whenKeyNotFoundForMerchant_throwsNotFoundException() {
            when(apiKeyRepository.findByIdAndMerchantId(20L, 2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> apiKeyService.revoke(2L, 20L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("API key not found");

            verify(apiKeyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("hashKey & lookup")
    class HashAndLookupTests {

        @Test
        @DisplayName("hashKey produces deterministic 64-character lowercase hex SHA-256 hash")
        void hashKey_producesExpectedSha256Hex() {
            String hash1 = apiKeyService.hashKey("sdk_sample_test_key_12345");
            String hash2 = apiKeyService.hashKey("sdk_sample_test_key_12345");

            assertThat(hash1).isEqualTo(hash2);
            assertThat(hash1).hasSize(64);
            assertThat(hash1).matches("^[0-9a-f]{64}$");

            // Empty string test with sha256
            String emptyHash = apiKeyService.hashKey("hello");
            // SHA-256("hello") = 2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824
            assertThat(emptyHash).isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
        }

        @Test
        @DisplayName("hashKey throws BadRequestException on null or blank input")
        void hashKey_throwsOnNullOrBlank() {
            assertThatThrownBy(() -> apiKeyService.hashKey(null))
                    .isInstanceOf(BadRequestException.class);
            assertThatThrownBy(() -> apiKeyService.hashKey("   "))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("findActiveKeyByHash delegates to repository")
        void findActiveKeyByHash_delegatesToRepository() {
            String hash = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
            ApiKey key = ApiKey.builder().id(1L).keyHash(hash).build();
            when(apiKeyRepository.findByKeyHashAndActiveTrue(hash)).thenReturn(Optional.of(key));

            Optional<ApiKey> result = apiKeyService.findActiveKeyByHash(hash);

            assertThat(result).contains(key);
            verify(apiKeyRepository).findByKeyHashAndActiveTrue(hash);
        }
    }
}
