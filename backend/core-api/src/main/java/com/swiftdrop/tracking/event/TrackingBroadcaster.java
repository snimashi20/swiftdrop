package com.swiftdrop.tracking.event;

import com.swiftdrop.common.event.ParcelStatusChangedEvent;
import com.swiftdrop.tracking.dto.TrackingUpdateMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Pushes a status change to browsers watching that tracking number, after the DB commit. */
@Component
@RequiredArgsConstructor
public class TrackingBroadcaster {

    static final String TOPIC_PREFIX = "/topic/tracking/";

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(ParcelStatusChangedEvent event) {
        messagingTemplate.convertAndSend(
                TOPIC_PREFIX + event.trackingNumber(),
                new TrackingUpdateMessage(
                        event.trackingNumber(),
                        event.newStatus(),
                        event.changedAt().toString()));
    }
}
