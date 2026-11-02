package com.swiftdrop.tracking.event;

import com.swiftdrop.common.event.ParcelStatusChangedEvent;
import com.swiftdrop.tracking.dto.TrackingUpdateMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrackingBroadcasterTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void sendsStatusAndTimeOnlyToTheTrackingTopic() {
        TrackingBroadcaster broadcaster = new TrackingBroadcaster(messagingTemplate);
        ParcelStatusChangedEvent event = new ParcelStatusChangedEvent(
                UUID.randomUUID(), 7L, "SD7K2M9QX4LP", "PICKED_UP", "IN_TRANSIT",
                Instant.parse("2026-10-28T13:40:00.123Z"), "COURIER", 5L, null,
                "Delivered to Nimal Perera");

        broadcaster.onStatusChanged(event);

        ArgumentCaptor<TrackingUpdateMessage> message = ArgumentCaptor.forClass(TrackingUpdateMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/tracking/SD7K2M9QX4LP"), message.capture());
        assertEquals("SD7K2M9QX4LP", message.getValue().trackingNumber());
        assertEquals("IN_TRANSIT", message.getValue().status());
        assertEquals("2026-10-28T13:40:00.123Z", message.getValue().changedAt());
        // TrackingUpdateMessage has no note field, so the courier's free text is never broadcast.
    }
}
