package com.swiftdrop.notification.service;

import com.swiftdrop.notification.domain.NotificationLog;
import com.swiftdrop.notification.domain.NotificationStatus;
import com.swiftdrop.notification.domain.NotificationType;
import com.swiftdrop.notification.event.ParcelStatusChangedEvent;
import com.swiftdrop.notification.repository.NotificationLogRepository;
import com.swiftdrop.notification.service.CustomerDirectory.CustomerContact;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final CustomerDirectory customers;
    private final EmailNotifier emailNotifier;
    private final SmsNotifier smsNotifier;
    private final NotificationLogRepository logs;

    public void handle(ParcelStatusChangedEvent event) {
        CustomerContact customer = customers.findById(event.customerId())
                .orElseThrow(() -> new AmqpRejectAndDontRequeueException(
                        "No user with id " + event.customerId() + " for event " + event.eventId()));

        if (customer.email() != null && !customer.email().isBlank()) {
            deliver(event, NotificationType.EMAIL, Masking.email(customer.email()),
                    () -> emailNotifier.send(customer.email(), event));
        }
        if (customer.phoneNumber() != null && !customer.phoneNumber().isBlank()) {
            deliver(event, NotificationType.SMS, Masking.phone(customer.phoneNumber()),
                    () -> smsNotifier.send(customer.phoneNumber(), event));
        }
    }

    private void deliver(ParcelStatusChangedEvent event, NotificationType type, String maskedRecipient, Runnable send) {
        if (logs.existsByEventIdAndType(event.eventId(), type)) {
            log.info("Skipping duplicate {} for event {}", type, event.eventId());
            return;
        }

        // Insert PENDING first so the unique (event_id, type) constraint claims the slot before anything is sent.
        NotificationLog row;
        try {
            row = logs.saveAndFlush(NotificationLog.builder()
                    .eventId(event.eventId())
                    .parcelId(event.parcelId())
                    .trackingNumber(event.trackingNumber())
                    .oldStatus(event.oldStatus())
                    .newStatus(event.newStatus())
                    .customerId(event.customerId())
                    .type(type)
                    .recipient(maskedRecipient)
                    .status(NotificationStatus.PENDING)
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.info("Skipping duplicate {} for event {} (claimed concurrently)", type, event.eventId());
            return;
        }

        try {
            send.run();
            row.setStatus(NotificationStatus.SENT);
            row.setSentAt(Instant.now());
            log.info("Sent {} for event {} to {}", type, event.eventId(), maskedRecipient);
        } catch (RuntimeException e) {
            // Exception messages can contain the raw address, so only the type is stored and logged.
            row.setStatus(NotificationStatus.FAILED);
            row.setFailureReason(e.getClass().getSimpleName());
            log.warn("Failed {} for event {}: {}", type, event.eventId(), e.getClass().getSimpleName());
        }
        logs.save(row);
    }
}
