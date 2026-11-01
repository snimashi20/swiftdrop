package com.swiftdrop.notification.event;

import com.swiftdrop.notification.config.RabbitConfig;
import com.swiftdrop.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParcelStatusListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitConfig.NOTIFICATION_QUEUE)
    public void onParcelStatusChanged(ParcelStatusChangedEvent event) {
        if (event.eventId() == null || event.parcelId() == null || event.customerId() == null
                || event.trackingNumber() == null || event.newStatus() == null) {
            throw new AmqpRejectAndDontRequeueException("Event is missing required fields");
        }
        notificationService.handle(event);
    }
}
