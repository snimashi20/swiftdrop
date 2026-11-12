package com.swiftdrop.webhook.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebhookSignerTest {

    private WebhookSigner signer;

    @BeforeEach
    void setUp() {
        signer = new WebhookSigner();
    }

    @Test
    @DisplayName("Should sign a payload and produce a valid lowercase hex HMAC-SHA256")
    void shouldSignPayloadCorrectly() {
        String payload = "{\"trackingNumber\":\"SD7K2M9QX4LP\",\"newStatus\":\"DELIVERED\"}";
        String secret = "test-secret-key-12345";

        String signature = signer.sign(payload, secret);

        assertThat(signature).isNotNull();
        assertThat(signature).matches("^[a-f0-9]{64}$");
    }

    @Test
    @DisplayName("Should verify valid signature successfully")
    void shouldVerifyValidSignature() {
        String payload = "{\"trackingNumber\":\"SD7K2M9QX4LP\",\"newStatus\":\"CANCELLED\"}";
        String secret = "merchant-secret-key";

        String signature = signer.sign(payload, secret);

        assertThat(signer.verify(payload, signature, secret)).isTrue();
        // Also case-insensitive check
        assertThat(signer.verify(payload, signature.toUpperCase(), secret)).isTrue();
    }

    @Test
    @DisplayName("Should reject tampered payload or incorrect signature")
    void shouldRejectTamperedPayload() {
        String originalPayload = "{\"trackingNumber\":\"SD7K2M9QX4LP\",\"newStatus\":\"PICKED_UP\"}";
        String tamperedPayload = "{\"trackingNumber\":\"SD7K2M9QX4LP\",\"newStatus\":\"DELIVERED\"}";
        String secret = "merchant-secret-key";

        String signature = signer.sign(originalPayload, secret);

        assertThat(signer.verify(tamperedPayload, signature, secret)).isFalse();
        assertThat(signer.verify(originalPayload, "invalid_signature", secret)).isFalse();
        assertThat(signer.verify(originalPayload, signature, "wrong-secret")).isFalse();
    }

    @Test
    @DisplayName("Should return false on null inputs during verification")
    void shouldHandleNullsSafelyInVerify() {
        assertThat(signer.verify(null, "sig", "sec")).isFalse();
        assertThat(signer.verify("payload", null, "sec")).isFalse();
        assertThat(signer.verify("payload", "sig", null)).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when signing with null inputs")
    void shouldThrowWhenSigningNullInputs() {
        assertThatThrownBy(() -> signer.sign(null, "secret"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> signer.sign("payload", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should generate unique, high-entropy secrets prefixed with whsec_")
    void shouldGenerateSecureSecrets() {
        String secret1 = signer.generateSecret();
        String secret2 = signer.generateSecret();

        assertThat(secret1).startsWith("whsec_");
        assertThat(secret2).startsWith("whsec_");
        assertThat(secret1).hasSize(6 + 48); // prefix + 24 bytes in hex
        assertThat(secret1).isNotEqualTo(secret2);
    }
}
