package com.swiftdrop.common.event;

import com.swiftdrop.common.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParcelStatusEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    // AFTER_COMMIT: listeners never hear about a status change that was rolled back.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(ParcelStatusChangedEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, event);
            log.info("Published status event {} for parcel {} ({} -> {})",
                    event.eventId(), event.parcelId(), event.oldStatus(), event.newStatus());
        } catch (AmqpException e) {
            // The status change is already committed; a broker outage must not surface as a failed request.
            log.error("Could not publish status event {} for parcel {}: {}",
                    event.eventId(), event.parcelId(), e.getClass().getSimpleName());
        }
    }
}
