package com.swiftdrop.webhook.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftdrop.common.config.RabbitMQConfig;
import com.swiftdrop.common.event.ParcelStatusChangedEvent;
import com.swiftdrop.webhook.service.WebhookDispatcherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookEventListener {

    private final WebhookDispatcherService webhookDispatcherService;
    private final ObjectMapper objectMapper;

    /**
     * Consumes status change events from the webhook.parcel-status queue.
     * Throws AmqpRejectAndDontRequeueException on unrecoverable bad data to move the message to DLQ.
     */
    @RabbitListener(queues = RabbitMQConfig.WEBHOOK_QUEUE)
    public void onParcelStatusChanged(String rawEventJson) {
        log.info("Received event on queue [{}]: {}", RabbitMQConfig.WEBHOOK_QUEUE, rawEventJson);

        ParcelStatusChangedEvent event;
        try {
            event = objectMapper.readValue(rawEventJson, ParcelStatusChangedEvent.class);
        } catch (Exception ex) {
            log.error("Failed to parse event JSON from queue {}. Rejecting to DLQ: {}",
                    RabbitMQConfig.WEBHOOK_QUEUE, ex.getMessage());
            // Poison message: route directly to webhook.parcel-status.dlq
            throw new AmqpRejectAndDontRequeueException("Unparseable webhook event message", ex);
        }

        if (event == null || event.trackingNumber() == null) {
            log.warn("Discarding malformed or empty ParcelStatusChangedEvent to DLQ");
            throw new AmqpRejectAndDontRequeueException("Malformed event missing trackingNumber");
        }

        log.info("Processing webhook for parcel {} (Status: {} -> {})",
                event.trackingNumber(), event.oldStatus(), event.newStatus());

        webhookDispatcherService.dispatch(event);
    }
}
