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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApiKeyService {

    private static final String KEY_PREFIX = "sdk_";
    private static final int RANDOM_BYTES_COUNT = 30; // 30 bytes Base64-URL encoded = exactly 40 chars
    private static final int PREFIX_DISPLAY_LENGTH = 12; // "sdk_" + 8 characters = 12 chars
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ApiKeyRepository apiKeyRepository;
    private final MerchantRepository merchantRepository;
    private final ApiKeyMapper apiKeyMapper;

    /**
     * Generates a new API key for the specified merchant.
     * The plain key is returned once in the response and is never stored or logged.
     * Only approved merchants can generate API keys.
     */
    @Transactional
    public ApiKeyGeneratedResponse generate(Long merchantId, String label) {
        if (label == null || label.isBlank()) {
            throw new BadRequestException("API key label is required");
        }
        if (label.trim().length() > 100) {
            throw new BadRequestException("API key label must not exceed 100 characters");
        }

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new NotFoundException("Merchant not found: " + merchantId));

        if (merchant.getStatus() != MerchantStatus.APPROVED) {
            throw new MerchantNotApprovedException("Merchant is not approved (current status: " + merchant.getStatus() + ")");
        }

        // Generate 40 URL-safe random characters
        byte[] randomBytes = new byte[RANDOM_BYTES_COUNT];
        SECURE_RANDOM.nextBytes(randomBytes);
        String randomPart = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String rawKey = KEY_PREFIX + randomPart;

        String keyPrefix = rawKey.substring(0, PREFIX_DISPLAY_LENGTH);
        String keyHash = hashKey(rawKey);

        ApiKey apiKey = ApiKey.builder()
                .merchant(merchant)
                .label(label.trim())
                .keyPrefix(keyPrefix)
                .keyHash(keyHash)
                .active(true)
                .createdAt(Instant.now())
                .build();

        ApiKey saved = apiKeyRepository.save(apiKey);
        log.info("Generated new API key id={} with prefix={} for merchantId={}", saved.getId(), keyPrefix, merchantId);

        return apiKeyMapper.toGeneratedResponse(saved, rawKey);
    }

    /**
     * Lists all API keys belonging to the specified merchant in descending creation order.
     * Does not contain plain keys or key hashes.
     */
    public List<ApiKeyResponse> list(Long merchantId) {
        if (!merchantRepository.existsById(merchantId)) {
            throw new NotFoundException("Merchant not found: " + merchantId);
        }
        List<ApiKey> keys = apiKeyRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
        return apiKeyMapper.toResponseList(keys);
    }

    /**
     * Revokes an existing API key for the specified merchant owner.
     */
    @Transactional
    public ApiKeyResponse revoke(Long merchantId, Long apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findByIdAndMerchantId(apiKeyId, merchantId)
                .orElseThrow(() -> new NotFoundException("API key not found"));

        apiKey.setActive(false);
        apiKey.setRevokedAt(Instant.now());
        ApiKey saved = apiKeyRepository.save(apiKey);

        log.info("Revoked API key id={} for merchantId={}", saved.getId(), merchantId);
        return apiKeyMapper.toResponse(saved);
    }

    /**
     * Computes the SHA-256 lowercase hex hash of the given raw API key.
     */
    public String hashKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new BadRequestException("API key cannot be empty");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Looks up an active API key by its SHA-256 hash.
     */
    public Optional<ApiKey> findActiveKeyByHash(String keyHash) {
        return apiKeyRepository.findByKeyHashAndActiveTrue(keyHash);
    }
}
