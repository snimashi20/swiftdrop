package com.swiftdrop.webhook.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftdrop.common.event.ParcelStatusChangedEvent;
import com.swiftdrop.webhook.service.WebhookDispatcherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookEventListenerTest {

    @Mock
    private WebhookDispatcherService webhookDispatcherService;

    @Mock
    private ObjectMapper objectMapper;

    private WebhookEventListener eventListener;

    @BeforeEach
    void setUp() {
        eventListener = new WebhookEventListener(webhookDispatcherService, objectMapper);
    }

    @Test
    @DisplayName("Should successfully consume event and dispatch to webhook engine")
    void shouldDispatchValidParcelStatusEvent() throws Exception {
        String json = "{\"trackingNumber\":\"SD7K2M9QX4LP\"}";
        ParcelStatusChangedEvent event = new ParcelStatusChangedEvent(
                UUID.randomUUID(),
                1L,
                "SD7K2M9QX4LP",
                "PICKED_UP",
                "IN_TRANSIT",
                Instant.now(),
                "COURIER",
                10L,
                20L,
                "Moving"
        );

        when(objectMapper.readValue(json, ParcelStatusChangedEvent.class)).thenReturn(event);

        eventListener.onParcelStatusChanged(json);

        verify(webhookDispatcherService).dispatch(event);
    }

    @Test
    @DisplayName("Should reject unparseable poison message to Dead-Letter Queue (DLQ)")
    void shouldRejectUnparseableMessageToDlq() throws Exception {
        String poisonJson = "{invalid-json-payload}";
        when(objectMapper.readValue(poisonJson, ParcelStatusChangedEvent.class))
                .thenThrow(new JsonProcessingException("Syntax error") {});

        assertThatThrownBy(() -> eventListener.onParcelStatusChanged(poisonJson))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("Unparseable webhook event message");

        verify(webhookDispatcherService, never()).dispatch(any());
    }

    @Test
    @DisplayName("Should reject event missing trackingNumber to Dead-Letter Queue (DLQ)")
    void shouldRejectMalformedEventMissingTrackingNumber() throws Exception {
        String json = "{\"merchantId\":20}";
        ParcelStatusChangedEvent malformedEvent = new ParcelStatusChangedEvent(
                UUID.randomUUID(),
                1L,
                null, // missing trackingNumber
                "CREATED",
                "PICKED_UP",
                Instant.now(),
                "DISPATCHER",
                10L,
                20L,
                null
        );

        when(objectMapper.readValue(json, ParcelStatusChangedEvent.class)).thenReturn(malformedEvent);

        assertThatThrownBy(() -> eventListener.onParcelStatusChanged(json))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("Malformed event missing trackingNumber");

        verify(webhookDispatcherService, never()).dispatch(any());
    }

    @Test
    @DisplayName("Should reject null event to Dead-Letter Queue (DLQ)")
    void shouldRejectNullEventToDlq() throws Exception {
        String json = "null";
        when(objectMapper.readValue(json, ParcelStatusChangedEvent.class)).thenReturn(null);

        assertThatThrownBy(() -> eventListener.onParcelStatusChanged(json))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("Malformed event missing trackingNumber");

        verify(webhookDispatcherService, never()).dispatch(any());
    }
}

