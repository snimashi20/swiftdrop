package com.swiftdrop.notification.event;

import com.swiftdrop.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * The queue entry point. Anything missing a required field must be rejected without requeue so it
 * lands in the dead-letter queue instead of looping forever.
 */
@ExtendWith(MockitoExtension.class)
class ParcelStatusListenerTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final Instant CHANGED_AT = Instant.parse("2026-11-14T10:15:30Z");

    @Mock NotificationService notificationService;

    @InjectMocks ParcelStatusListener listener;

    @Test
    @DisplayName("a complete event is handed to the notification service")
    void completeEventIsHandled() {
        ParcelStatusChangedEvent event = event(EVENT_ID, 42L, "SD7K2M9QX4LP", "PICKED_UP", "IN_TRANSIT", 7L);

        listener.onParcelStatusChanged(event);

        verify(notificationService).handle(event);
    }

    @Test
    @DisplayName("a missing oldStatus is allowed: the first event of a parcel is still handled")
    void nullOldStatusIsStillHandled() {
        ParcelStatusChangedEvent event = event(EVENT_ID, 42L, "SD7K2M9QX4LP", null, "CREATED", 7L);

        listener.onParcelStatusChanged(event);

        verify(notificationService).handle(event);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("incompleteEvents")
    @DisplayName("an event missing a required field is rejected without requeue")
    void incompleteEventIsRejectedWithoutRequeue(String missingField, ParcelStatusChangedEvent event) {
        assertThatThrownBy(() -> listener.onParcelStatusChanged(event))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessage("Event is missing required fields");

        verifyNoInteractions(notificationService);
    }

    private static Stream<Arguments> incompleteEvents() {
        return Stream.of(
                Arguments.of("no eventId", event(null, 42L, "SD7K2M9QX4LP", "PICKED_UP", "IN_TRANSIT", 7L)),
                Arguments.of("no parcelId", event(EVENT_ID, null, "SD7K2M9QX4LP", "PICKED_UP", "IN_TRANSIT", 7L)),
                Arguments.of("no trackingNumber", event(EVENT_ID, 42L, null, "PICKED_UP", "IN_TRANSIT", 7L)),
                Arguments.of("no newStatus", event(EVENT_ID, 42L, "SD7K2M9QX4LP", "PICKED_UP", null, 7L)),
                Arguments.of("no customerId", event(EVENT_ID, 42L, "SD7K2M9QX4LP", "PICKED_UP", "IN_TRANSIT", null)));
    }

    private static ParcelStatusChangedEvent event(UUID eventId, Long parcelId, String trackingNumber,
                                                  String oldStatus, String newStatus, Long customerId) {
        return new ParcelStatusChangedEvent(eventId, parcelId, trackingNumber, oldStatus, newStatus,
                CHANGED_AT, "COURIER", customerId, 99L, "On the way");
    }
}
