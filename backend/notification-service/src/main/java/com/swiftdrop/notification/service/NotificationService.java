package com.swiftdrop.notification.service;

import com.swiftdrop.notification.domain.NotificationLog;
import com.swiftdrop.notification.domain.NotificationStatus;
import com.swiftdrop.notification.domain.NotificationType;
import com.swiftdrop.notification.event.ParcelStatusChangedEvent;
import com.swiftdrop.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Turns one status change into the notifications it owes each of its recipients.
 *
 * <p>{@link RecipientDirectory} decides <em>who</em> hears about the event and
 * {@link NotificationPolicy} decides <em>on which channels</em>; this class only carries it out and
 * records the outcome. In practice that means the customer may be e-mailed and texted, while the
 * merchant, the assigned courier and the dispatch/admin staff get an in-app row and nothing else.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    /**
     * What an in-app row stores as its recipient. The row is addressed by {@code recipient_user_id},
     * so the column has nothing useful to hold here and no reason to carry an address.
     */
    private static final String IN_APP_RECIPIENT = "in-app";

    private final RecipientDirectory recipients;
    private final EmailNotifier emailNotifier;
    private final SmsNotifier smsNotifier;
    private final NotificationLogRepository logs;

    public void handle(ParcelStatusChangedEvent event) {
        List<NotificationRecipient> targets = recipients.resolve(event);
        if (targets.isEmpty()) {
            throw new AmqpRejectAndDontRequeueException(
                    "No recipient could be resolved for event " + event.eventId());
        }
        for (NotificationRecipient target : targets) {
            notify(event, target);
        }
    }

    /** Runs every channel the recipient's role allows; one failing channel never stops the next. */
    private void notify(ParcelStatusChangedEvent event, NotificationRecipient target) {
        for (NotificationType channel : NotificationPolicy.channelsFor(target.role())) {
            switch (channel) {
                // No gateway: writing the row is the delivery, and the feed endpoints hand it over.
                case IN_APP -> deliver(event, target, NotificationType.IN_APP, IN_APP_RECIPIENT, () -> {
                });
                case EMAIL -> {
                    if (hasText(target.email())) {
                        deliver(event, target, NotificationType.EMAIL, Masking.email(target.email()),
                                () -> emailNotifier.send(target.email(), event));
                    }
                }
                case SMS -> {
                    if (hasText(target.phoneNumber())) {
                        deliver(event, target, NotificationType.SMS, Masking.phone(target.phoneNumber()),
                                () -> smsNotifier.send(target.phoneNumber(), event));
                    }
                }
            }
        }
    }

    private void deliver(ParcelStatusChangedEvent event, NotificationRecipient target,
                         NotificationType type, String maskedRecipient, Runnable send) {
        if (logs.existsByEventIdAndRecipientUserIdAndType(event.eventId(), target.userId(), type)) {
            log.info("Skipping duplicate {} for event {} to user {}", type, event.eventId(), target.userId());
            return;
        }

        // Insert PENDING first so the unique (event_id, recipient_user_id, type) constraint claims the
        // slot before anything is sent.
        NotificationLog row;
        try {
            row = logs.saveAndFlush(NotificationLog.builder()
                    .eventId(event.eventId())
                    .parcelId(event.parcelId())
                    .trackingNumber(event.trackingNumber())
                    .oldStatus(event.oldStatus())
                    .newStatus(event.newStatus())
                    .customerId(event.customerId())
                    .recipientUserId(target.userId())
                    .recipientRole(target.role())
                    .parcelFailureReason(event.failureReason())
                    .type(type)
                    .recipient(maskedRecipient)
                    .status(NotificationStatus.PENDING)
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.info("Skipping duplicate {} for event {} to user {} (claimed concurrently)",
                    type, event.eventId(), target.userId());
            return;
        }

        try {
            send.run();
            row.setStatus(NotificationStatus.SENT);
            row.setSentAt(Instant.now());
            log.info("Sent {} for event {} to {}", type, event.eventId(), maskedRecipient);
        } catch (RuntimeException e) {
            // Only the exception type is stored: the column is read back by the admin log page, and a
            // gateway message can quote the raw address. The log line carries the gateway's own reason
            // with addresses masked, because without it a failure is undiagnosable - "MailSendException"
            // alone cannot tell a rejected password from an unreachable host.
            row.setStatus(NotificationStatus.FAILED);
            row.setFailureReason(e.getClass().getSimpleName());
            log.warn("Failed {} for event {} to {}: {} - {}", type, event.eventId(), maskedRecipient,
                    e.getClass().getSimpleName(), Masking.text(rootCauseMessage(e)));
        }
        logs.save(row);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * The deepest message in the chain, which is where a gateway puts its own reason. Spring wraps
     * the provider's exception, so the outer message is usually just "Mail server connection failed".
     */
    private static String rootCauseMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message == null ? cause.getClass().getSimpleName() : message;
    }
}
