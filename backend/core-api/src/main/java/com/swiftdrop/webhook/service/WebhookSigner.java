package com.swiftdrop.webhook.service;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

@Component
public class WebhookSigner {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SECRET_PREFIX = "whsec_";
    private static final int SECRET_BYTE_LENGTH = 24;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Computes the HMAC-SHA256 signature for a raw payload string using the given secret key.
     *
     * @param payload the serialized JSON body
     * @param secret  the shared merchant webhook secret
     * @return lowercase hex-encoded HMAC-SHA256 digest
     */
    public String sign(String payload, String secret) {
        if (payload == null || secret == null) {
            throw new IllegalArgumentException("Payload and secret must not be null");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rawHmac);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256 signature", e);
        }
    }

    /**
     * Verifies an incoming signature header against the computed signature in constant time
     * to protect against timing side-channel attacks.
     *
     * @param payload         the raw body payload
     * @param signatureHeader the signature provided in HTTP header (e.g., X-SwiftDrop-Signature)
     * @param secret          the shared secret
     * @return true if valid, false otherwise
     */
    public boolean verify(String payload, String signatureHeader, String secret) {
        if (payload == null || signatureHeader == null || secret == null) {
            return false;
        }
        String expectedSignature = sign(payload, secret);
        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signatureHeader.trim().toLowerCase().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Generates a cryptographically strong, high-entropy shared secret for new merchant endpoints.
     *
     * @return a secret formatted as "whsec_<48-hex-chars>"
     */
    public String generateSecret() {
        byte[] randomBytes = new byte[SECRET_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return SECRET_PREFIX + HexFormat.of().formatHex(randomBytes);
    }
}
