package com.swiftdrop.webhook.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookDomainEntitiesTest {

    @Test
    @DisplayName("WebhookDelivery prePersist should assign defaults when fields are null")
    void testWebhookDeliveryPrePersist() {
        WebhookDelivery delivery = new WebhookDelivery();
        // Null the fields to ensure prePersist assigns defaults rather than relying on @Builder.Default
        delivery.setCreatedAt(null);
        delivery.setStatus(null);
        delivery.setAttempts(null);
        delivery.prePersist();

        assertThat(delivery.getCreatedAt()).isNotNull();
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(delivery.getAttempts()).isEqualTo(0);

        // PrePersist should not overwrite existing values
        Instant fixed = Instant.now().minusSeconds(100);
        delivery.setCreatedAt(fixed);
        delivery.setStatus(DeliveryStatus.FAILED);
        delivery.setAttempts(3);
        delivery.prePersist();

        assertThat(delivery.getCreatedAt()).isEqualTo(fixed);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(delivery.getAttempts()).isEqualTo(3);
    }

    @Test
    @DisplayName("WebhookEndpoint prePersist should assign defaults when fields are null")
    void testWebhookEndpointPrePersist() {
        WebhookEndpoint endpoint = new WebhookEndpoint();
        // Null the fields to ensure prePersist assigns defaults rather than relying on @Builder.Default
        endpoint.setCreatedAt(null);
        endpoint.setActive(null);
        endpoint.prePersist();

        assertThat(endpoint.getCreatedAt()).isNotNull();
        assertThat(endpoint.getActive()).isTrue();

        Instant fixed = Instant.now().minusSeconds(50);
        endpoint.setCreatedAt(fixed);
        endpoint.setActive(false);
        endpoint.prePersist();

        assertThat(endpoint.getCreatedAt()).isEqualTo(fixed);
        assertThat(endpoint.getActive()).isFalse();
    }
}
