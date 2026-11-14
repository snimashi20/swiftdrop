package com.swiftdrop.webhook.service;

import com.swiftdrop.webhook.domain.DeliveryStatus;
import com.swiftdrop.webhook.domain.WebhookDelivery;
import com.swiftdrop.webhook.repository.WebhookDeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookRetrySchedulerTest {

    @Mock
    private WebhookDeliveryRepository deliveryRepository;

    @Mock
    private WebhookDispatcherService dispatcherService;

    private WebhookRetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new WebhookRetryScheduler(deliveryRepository, dispatcherService);
    }

    @Test
    @DisplayName("Should do nothing when no pending retries are due")
    void shouldDoNothingWhenNoDueRetries() {
        when(deliveryRepository.findByStatusAndNextRetryAtLessThanEqual(eq(DeliveryStatus.RETRYING), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        scheduler.processPendingRetries();

        verify(dispatcherService, never()).retryDelivery(any());
    }

    @Test
    @DisplayName("Should process all due retries via dispatcher service")
    void shouldProcessDueRetries() {
        WebhookDelivery d1 = WebhookDelivery.builder().id(1L).attempts(1).build();
        WebhookDelivery d2 = WebhookDelivery.builder().id(2L).attempts(2).build();

        when(deliveryRepository.findByStatusAndNextRetryAtLessThanEqual(eq(DeliveryStatus.RETRYING), any(Instant.class)))
                .thenReturn(List.of(d1, d2));

        scheduler.processPendingRetries();

        verify(dispatcherService).retryDelivery(d1);
        verify(dispatcherService).retryDelivery(d2);
    }

    @Test
    @DisplayName("Should catch exceptions and continue processing remaining retries")
    void shouldContinueWhenIndividualRetryThrows() {
        WebhookDelivery failingDelivery = WebhookDelivery.builder().id(10L).attempts(1).build();
        WebhookDelivery succeedingDelivery = WebhookDelivery.builder().id(20L).attempts(1).build();

        when(deliveryRepository.findByStatusAndNextRetryAtLessThanEqual(eq(DeliveryStatus.RETRYING), any(Instant.class)))
                .thenReturn(List.of(failingDelivery, succeedingDelivery));

        doThrow(new RuntimeException("Network timeout")).when(dispatcherService).retryDelivery(failingDelivery);

        scheduler.processPendingRetries();

        verify(dispatcherService).retryDelivery(failingDelivery);
        verify(dispatcherService).retryDelivery(succeedingDelivery);
    }
}

