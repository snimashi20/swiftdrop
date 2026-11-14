package com.swiftdrop.webhook.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
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

        Object factory = restTemplate.getRequestFactory();
        assertThat(ReflectionTestUtils.getField(factory, "connectTimeout")).isEqualTo(5000);
        assertThat(ReflectionTestUtils.getField(factory, "readTimeout")).isEqualTo(5000);
    }
}
