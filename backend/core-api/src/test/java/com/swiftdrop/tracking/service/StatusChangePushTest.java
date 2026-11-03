package com.swiftdrop.tracking.service;

import com.swiftdrop.common.event.ParcelStatusChangedEvent;
import com.swiftdrop.parcel.domain.Parcel;
import com.swiftdrop.parcel.domain.ParcelStatus;
import com.swiftdrop.parcel.repository.ParcelRepository;
import com.swiftdrop.tracking.dto.TrackingUpdateMessage;
import com.swiftdrop.tracking.event.TrackingBroadcaster;
import com.swiftdrop.tracking.repository.StatusHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Status change -> event -> WebSocket message, wired together without Spring. */
@ExtendWith(MockitoExtension.class)
class StatusChangePushTest {

    @Mock
    private ParcelRepository parcelRepository;
    @Mock
    private StatusHistoryRepository statusHistoryRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void changingAStatusPushesAMessageToThatParcelsTopic() {
        TrackingBroadcaster broadcaster = new TrackingBroadcaster(messagingTemplate);
        // Stands in for Spring's after-commit delivery of the event to the broadcaster.
        doAnswer(invocation -> {
            broadcaster.onStatusChanged(invocation.getArgument(0, ParcelStatusChangedEvent.class));
            return null;
        }).when(eventPublisher).publishEvent(any(ParcelStatusChangedEvent.class));

        ParcelStatusService service = new ParcelStatusService(
                parcelRepository, statusHistoryRepository, new StatusTransitionRules(), eventPublisher);

        Parcel parcel = new Parcel();
        parcel.setId(7L);
        parcel.setTrackingNumber("SD7K2M9QX4LP");
        parcel.setStatus(ParcelStatus.PICKED_UP);
        when(parcelRepository.findById(7L)).thenReturn(Optional.of(parcel));

        service.changeStatus(7L, ParcelStatus.IN_TRANSIT, "COURIER", 42L, "Delivered to Nimal Perera");

        ArgumentCaptor<TrackingUpdateMessage> message = ArgumentCaptor.forClass(TrackingUpdateMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/tracking/SD7K2M9QX4LP"), message.capture());
        assertEquals("IN_TRANSIT", message.getValue().status());
        assertEquals("SD7K2M9QX4LP", message.getValue().trackingNumber());
    }
}
