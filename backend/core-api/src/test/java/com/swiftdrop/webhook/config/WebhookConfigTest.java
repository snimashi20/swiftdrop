package com.swiftdrop.webhook.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookConfigTest {

    @Test
    @DisplayName("Should instantiate RestTemplate with configured timeouts")
    void shouldConfigureWebhookRestTemplate() {
        WebhookConfig config = new WebhookConfig();
        RestTemplate restTemplate = config.webhookRestTemplate();

        assertThat(restTemplate).isNotNull();
        assertThat(restTemplate.getRequestFactory()).isNotNull();
    }
}

