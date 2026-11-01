package com.swiftdrop.tracking.service;

import com.swiftdrop.common.event.ParcelStatusChangedEvent;
import com.swiftdrop.parcel.domain.Parcel;
import com.swiftdrop.parcel.domain.ParcelStatus;
import com.swiftdrop.parcel.repository.ParcelRepository;
import com.swiftdrop.tracking.repository.StatusHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParcelStatusServiceTest {

    @Mock ParcelRepository parcelRepository;
    @Mock StatusHistoryRepository statusHistoryRepository;
    @Spy StatusTransitionRules statusTransitionRules = new StatusTransitionRules();
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks ParcelStatusService service;

    @Test
    void raisesStatusEventWithContractFields() {
        Parcel parcel = new Parcel();
        parcel.setId(5L);
        parcel.setTrackingNumber("SD7K2M9QX4LP");
        parcel.setCustomerId(9L);
        parcel.setStatus(ParcelStatus.PICKED_UP);
        when(parcelRepository.findById(5L)).thenReturn(Optional.of(parcel));

        service.changeStatus(5L, "IN_TRANSIT", "COURIER", "Left the hub");

        ArgumentCaptor<ParcelStatusChangedEvent> captor = ArgumentCaptor.forClass(ParcelStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ParcelStatusChangedEvent event = captor.getValue();
        assertThat(event.eventId()).isNotNull();
        assertThat(event.parcelId()).isEqualTo(5L);
        assertThat(event.trackingNumber()).isEqualTo("SD7K2M9QX4LP");
        assertThat(event.oldStatus()).isEqualTo("PICKED_UP");
        assertThat(event.newStatus()).isEqualTo("IN_TRANSIT");
        assertThat(event.changedAt()).isNotNull();
        assertThat(event.changedByRole()).isEqualTo("COURIER");
        assertThat(event.customerId()).isEqualTo(9L);
        assertThat(event.merchantId()).isNull();
        assertThat(event.note()).isEqualTo("Left the hub");
    }

    @Test
    void refusedTransitionRaisesNoEvent() {
        Parcel parcel = new Parcel();
        parcel.setId(5L);
        parcel.setStatus(ParcelStatus.DELIVERED);
        when(parcelRepository.findById(5L)).thenReturn(Optional.of(parcel));

        assertThatThrownBy(() -> service.changeStatus(5L, "IN_TRANSIT", "COURIER", null))
                .isInstanceOf(InvalidStatusTransitionException.class);
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}
