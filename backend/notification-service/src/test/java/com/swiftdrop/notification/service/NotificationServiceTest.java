package com.swiftdrop.notification.service;

import com.swiftdrop.notification.domain.NotificationLog;
import com.swiftdrop.notification.domain.NotificationStatus;
import com.swiftdrop.notification.domain.NotificationType;
import com.swiftdrop.notification.event.ParcelStatusChangedEvent;
import com.swiftdrop.notification.repository.NotificationLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.MailSendException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The consumer-side orchestration: who a status change reaches, which channels each of them gets,
 * what lands in notification_log, and how duplicates and gateway failures are absorbed.
 *
 * <p>The channel split is the rule most worth protecting here: the customer is reachable
 * off-platform and everyone else is in-app only, so a merchant, courier, dispatcher or admin must
 * never produce a call to the e-mail or SMS gateway, even when their account has an address and a
 * phone number on it.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final Long CUSTOMER_USER = 7L;
    private static final Long MERCHANT_USER = 21L;
    private static final Long COURIER_USER = 33L;
    private static final Long DISPATCHER_USER = 50L;
    private static final Long ADMIN_USER = 88L;
    private static final String EMAIL = "sithmi@example.com";
    private static final String PHONE = "0771234567";

    @Mock RecipientDirectory recipients;
    @Mock EmailNotifier emailNotifier;
    @Mock SmsNotifier smsNotifier;
    @Mock NotificationLogRepository logs;

    @InjectMocks NotificationService service;

    // ----- the customer: in-app, e-mail and SMS -----

    @Test
    @DisplayName("a customer with an e-mail and a phone number gets all three channels, logged as SENT and masked")
    void customerGetsInAppEmailAndSms() {
        givenRecipients(customer(EMAIL, PHONE));
        givenSaveReturnsTheRow();
        ParcelStatusChangedEvent event = event("PICKED_UP", "IN_TRANSIT");

        service.handle(event);

        verify(emailNotifier).send(EMAIL, event);
        verify(smsNotifier).send(PHONE, event);

        List<NotificationLog> rows = savedRows(3);
        assertThat(rows).extracting(NotificationLog::getType)
                .containsExactlyInAnyOrder(NotificationType.IN_APP, NotificationType.EMAIL, NotificationType.SMS);
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.getRecipientUserId()).isEqualTo(CUSTOMER_USER);
            assertThat(row.getRecipientRole()).isEqualTo("CUSTOMER");
            assertThat(row.getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(row.getEventId()).isEqualTo(EVENT_ID);
            assertThat(row.getParcelId()).isEqualTo(42L);
            assertThat(row.getTrackingNumber()).isEqualTo("SD7K2M9QX4LP");
            assertThat(row.getCustomerId()).isEqualTo(CUSTOMER_USER);
        });

        NotificationLog emailRow = rowOfType(rows, NotificationType.EMAIL);
        assertThat(emailRow.getRecipient()).isEqualTo("s***@example.com");
        assertThat(emailRow.getSentAt()).isNotNull();
        assertThat(emailRow.getFailureReason()).isNull();
        assertThat(emailRow.getOldStatus()).isEqualTo("PICKED_UP");
        assertThat(emailRow.getNewStatus()).isEqualTo("IN_TRANSIT");

        assertThat(rowOfType(rows, NotificationType.SMS).getRecipient()).isEqualTo("*******567");

        // An in-app row is addressed by recipient_user_id, so it carries no address at all.
        assertThat(rowOfType(rows, NotificationType.IN_APP).getRecipient()).isEqualTo("in-app");

        // The raw address must never reach the log table.
        assertThat(rows).extracting(NotificationLog::getRecipient).doesNotContain(EMAIL, PHONE);
    }

    @Test
    @DisplayName("an event with no previous status (parcel just created) is delivered with oldStatus null")
    void firstEventOfAParcelIsDeliveredWithNullOldStatus() {
        givenRecipients(customer(EMAIL, null));
        givenSaveReturnsTheRow();
        ParcelStatusChangedEvent event = event(null, "CREATED");

        service.handle(event);

        List<NotificationLog> rows = savedRows(2);
        assertThat(rows).allSatisfy(row -> assertThat(row.getOldStatus()).isNull());
        assertThat(rows).extracting(NotificationLog::getNewStatus).containsOnly("CREATED");
        // No special-casing in production: the text is the standard "is now <status>" line.
        assertThat(EmailNotifier.subject(event))
                .isEqualTo("SwiftDrop: parcel SD7K2M9QX4LP is now created");
    }

    // ----- everyone else: in-app only -----

    @Test
    @DisplayName("a merchant with an e-mail and a phone number is notified in-app only, and no gateway is called")
    void merchantIsInAppOnly() {
        givenRecipients(new NotificationRecipient(MERCHANT_USER, "MERCHANT", EMAIL, PHONE));
        givenSaveReturnsTheRow();

        service.handle(event("PICKED_UP", "IN_TRANSIT"));

        verifyNoInteractions(emailNotifier, smsNotifier);
        NotificationLog row = savedRows(1).get(0);
        assertThat(row.getType()).isEqualTo(NotificationType.IN_APP);
        assertThat(row.getRecipientUserId()).isEqualTo(MERCHANT_USER);
        assertThat(row.getRecipientRole()).isEqualTo("MERCHANT");
        assertThat(row.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(row.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("a courier is notified in-app only, even with contact details on the account")
    void courierIsInAppOnly() {
        givenRecipients(new NotificationRecipient(COURIER_USER, "COURIER", EMAIL, PHONE));
        givenSaveReturnsTheRow();

        service.handle(event("IN_TRANSIT", "OUT_FOR_DELIVERY"));

        verifyNoInteractions(emailNotifier, smsNotifier);
        assertThat(savedRows(1).get(0).getType()).isEqualTo(NotificationType.IN_APP);
    }

    @Test
    @DisplayName("a dispatcher and an admin are notified in-app only")
    void staffAreInAppOnly() {
        givenRecipients(
                new NotificationRecipient(DISPATCHER_USER, "DISPATCHER", EMAIL, PHONE),
                new NotificationRecipient(ADMIN_USER, "ADMIN", EMAIL, PHONE));
        givenSaveReturnsTheRow();

        service.handle(event("IN_TRANSIT", "FAILED"));

        verifyNoInteractions(emailNotifier, smsNotifier);
        List<NotificationLog> rows = savedRows(2);
        assertThat(rows).extracting(NotificationLog::getType).containsOnly(NotificationType.IN_APP);
        assertThat(rows).extracting(NotificationLog::getRecipientRole)
                .containsExactlyInAnyOrder("DISPATCHER", "ADMIN");
    }

    @Test
    @DisplayName("one status change fans out to every recipient, with only the customer e-mailed and texted")
    void oneEventFansOutToEveryRecipient() {
        givenRecipients(
                customer(EMAIL, PHONE),
                new NotificationRecipient(MERCHANT_USER, "MERCHANT", "shop@example.com", "0770000000"),
                new NotificationRecipient(COURIER_USER, "COURIER", "rider@example.com", "0771111111"),
                new NotificationRecipient(ADMIN_USER, "ADMIN", "admin@example.com", "0772222222"));
        givenSaveReturnsTheRow();
        ParcelStatusChangedEvent event = event("OUT_FOR_DELIVERY", "FAILED");

        service.handle(event);

        // Three rows for the customer, one in-app row for each of the other three.
        List<NotificationLog> rows = savedRows(6);
        assertThat(rows).filteredOn(row -> row.getType() != NotificationType.IN_APP)
                .extracting(NotificationLog::getRecipientUserId)
                .containsOnly(CUSTOMER_USER);
        assertThat(rows).filteredOn(row -> row.getType() == NotificationType.IN_APP)
                .extracting(NotificationLog::getRecipientUserId)
                .containsExactlyInAnyOrder(CUSTOMER_USER, MERCHANT_USER, COURIER_USER, ADMIN_USER);

        // Exactly one e-mail and one SMS left the system, both to the customer.
        verify(emailNotifier).send(EMAIL, event);
        verify(smsNotifier).send(PHONE, event);
        verify(emailNotifier, never()).send(eq("shop@example.com"), any());
        verify(emailNotifier, never()).send(eq("rider@example.com"), any());
        verify(emailNotifier, never()).send(eq("admin@example.com"), any());
    }

    @Test
    @DisplayName("a failure stores the parcel's reason on every row, kept apart from the send's own failure reason")
    void failureReasonIsStoredOnEveryRow() {
        givenRecipients(customer(EMAIL, PHONE), new NotificationRecipient(ADMIN_USER, "ADMIN", null, null));
        givenSaveReturnsTheRow();

        service.handle(failureEvent("RECIPIENT_NOT_AVAILABLE"));

        List<NotificationLog> rows = savedRows(4);
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.getParcelFailureReason()).isEqualTo("RECIPIENT_NOT_AVAILABLE");
            // The send succeeded, so the column about this row's own delivery stays empty.
            assertThat(row.getFailureReason()).isNull();
        });
    }

    @Test
    @DisplayName("a status change that is not a failure records no parcel failure reason")
    void nonFailureStoresNoReason() {
        givenRecipients(customer(EMAIL, null));
        givenSaveReturnsTheRow();

        service.handle(event("PICKED_UP", "IN_TRANSIT"));

        assertThat(savedRows(2)).allSatisfy(row ->
                assertThat(row.getParcelFailureReason()).isNull());
    }

    // ----- missing / blank contact details -----

    @Test
    @DisplayName("a customer with no phone number gets in-app and e-mail only")
    void nullPhoneNumberSkipsSms() {
        givenRecipients(customer(EMAIL, null));
        givenSaveReturnsTheRow();
        ParcelStatusChangedEvent event = event("PICKED_UP", "IN_TRANSIT");

        service.handle(event);

        verify(emailNotifier).send(EMAIL, event);
        verifyNoInteractions(smsNotifier);
        assertThat(savedRows(2)).extracting(NotificationLog::getType)
                .containsExactlyInAnyOrder(NotificationType.IN_APP, NotificationType.EMAIL);
    }

    @Test
    @DisplayName("a blank e-mail address gets in-app and SMS only")
    void blankEmailSkipsEmail() {
        givenRecipients(customer("   ", PHONE));
        givenSaveReturnsTheRow();
        ParcelStatusChangedEvent event = event("PICKED_UP", "IN_TRANSIT");

        service.handle(event);

        verifyNoInteractions(emailNotifier);
        verify(smsNotifier).send(PHONE, event);
        assertThat(savedRows(2)).extracting(NotificationLog::getType)
                .containsExactlyInAnyOrder(NotificationType.IN_APP, NotificationType.SMS);
    }

    @Test
    @DisplayName("a customer with neither e-mail nor phone number still sees it in the app")
    void customerWithNoContactDetailsStillGetsTheInAppRow() {
        givenRecipients(customer(null, "  "));
        givenSaveReturnsTheRow();

        service.handle(event("PICKED_UP", "IN_TRANSIT"));

        verifyNoInteractions(emailNotifier, smsNotifier);
        assertThat(savedRows(1).get(0).getType()).isEqualTo(NotificationType.IN_APP);
    }

    @Test
    @DisplayName("an event nobody can be resolved for is rejected without requeue and nothing is logged")
    void eventWithNoRecipientsIsRejectedWithoutRequeue() {
        when(recipients.resolve(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.handle(event("PICKED_UP", "IN_TRANSIT")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("No recipient could be resolved")
                .hasMessageContaining(EVENT_ID.toString());

        verifyNoInteractions(emailNotifier, smsNotifier, logs);
    }

    @Test
    @DisplayName("a rejection raised while resolving recipients is left to reach the dead-letter queue")
    void rejectionFromTheDirectoryIsNotSwallowed() {
        when(recipients.resolve(any()))
                .thenThrow(new AmqpRejectAndDontRequeueException("No user with id 7 for event " + EVENT_ID));

        assertThatThrownBy(() -> service.handle(event("PICKED_UP", "IN_TRANSIT")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("No user with id 7");

        verifyNoInteractions(emailNotifier, smsNotifier, logs);
    }

    // ----- duplicates -----

    @Test
    @DisplayName("a redelivered event is not sent twice")
    void alreadyLoggedEventIsSkipped() {
        givenRecipients(customer(EMAIL, PHONE));
        when(logs.existsByEventIdAndRecipientUserIdAndType(eq(EVENT_ID), eq(CUSTOMER_USER), any()))
                .thenReturn(true);

        service.handle(event("PICKED_UP", "IN_TRANSIT"));

        verifyNoInteractions(emailNotifier, smsNotifier);
        verify(logs, never()).saveAndFlush(any());
        verify(logs, never()).save(any());
    }

    @Test
    @DisplayName("when only one channel was already logged the other ones still go out")
    void onlyTheMissingChannelsAreRetried() {
        givenRecipients(customer(EMAIL, PHONE));
        when(logs.existsByEventIdAndRecipientUserIdAndType(EVENT_ID, CUSTOMER_USER, NotificationType.IN_APP))
                .thenReturn(true);
        when(logs.existsByEventIdAndRecipientUserIdAndType(EVENT_ID, CUSTOMER_USER, NotificationType.EMAIL))
                .thenReturn(true);
        when(logs.existsByEventIdAndRecipientUserIdAndType(EVENT_ID, CUSTOMER_USER, NotificationType.SMS))
                .thenReturn(false);
        givenSaveReturnsTheRow();
        ParcelStatusChangedEvent event = event("PICKED_UP", "IN_TRANSIT");

        service.handle(event);

        verifyNoInteractions(emailNotifier);
        verify(smsNotifier).send(PHONE, event);
        assertThat(savedRows(1).get(0).getType()).isEqualTo(NotificationType.SMS);
    }

    @Test
    @DisplayName("one recipient's existing row does not suppress another recipient's copy of the event")
    void duplicateCheckIsPerRecipient() {
        givenRecipients(
                customer(null, null),
                new NotificationRecipient(MERCHANT_USER, "MERCHANT", null, null));
        when(logs.existsByEventIdAndRecipientUserIdAndType(EVENT_ID, CUSTOMER_USER, NotificationType.IN_APP))
                .thenReturn(true);
        when(logs.existsByEventIdAndRecipientUserIdAndType(EVENT_ID, MERCHANT_USER, NotificationType.IN_APP))
                .thenReturn(false);
        givenSaveReturnsTheRow();

        service.handle(event("PICKED_UP", "IN_TRANSIT"));

        assertThat(savedRows(1).get(0).getRecipientUserId()).isEqualTo(MERCHANT_USER);
    }

    @Test
    @DisplayName("a slot claimed concurrently by another consumer is skipped, not sent")
    void concurrentlyClaimedSlotIsSkipped() {
        givenRecipients(customer(EMAIL, null));
        when(logs.saveAndFlush(any(NotificationLog.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        service.handle(event("PICKED_UP", "IN_TRANSIT"));

        verifyNoInteractions(emailNotifier);
        verify(logs, never()).save(any());
    }

    // ----- gateway failures -----

    @Test
    @DisplayName("an e-mail gateway failure is logged as FAILED with the exception type only")
    void emailFailureIsLoggedAsFailed() {
        givenRecipients(customer(EMAIL, null));
        givenSaveReturnsTheRow();
        ParcelStatusChangedEvent event = event("PICKED_UP", "IN_TRANSIT");
        doThrow(new MailSendException("smtp refused sithmi@example.com"))
                .when(emailNotifier).send(EMAIL, event);

        service.handle(event);

        NotificationLog row = rowOfType(savedRows(2), NotificationType.EMAIL);
        assertThat(row.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(row.getFailureReason()).isEqualTo("MailSendException");
        assertThat(row.getSentAt()).isNull();
        // The failure reason must not leak the address the gateway echoed back.
        assertThat(row.getFailureReason()).doesNotContain(EMAIL);
    }

    @Test
    @DisplayName("a failing e-mail does not stop the SMS or the in-app row for the same event")
    void otherChannelsStillGoOutWhenEmailFails() {
        givenRecipients(customer(EMAIL, PHONE));
        givenSaveReturnsTheRow();
        ParcelStatusChangedEvent event = event("PICKED_UP", "IN_TRANSIT");
        doThrow(new MailSendException("smtp down")).when(emailNotifier).send(EMAIL, event);

        service.handle(event);

        verify(smsNotifier).send(PHONE, event);
        List<NotificationLog> rows = savedRows(3);
        assertThat(rowOfType(rows, NotificationType.EMAIL).getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(rowOfType(rows, NotificationType.SMS).getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(rowOfType(rows, NotificationType.IN_APP).getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    @DisplayName("a channel failing for one recipient does not stop the next recipient")
    void aFailureForOneRecipientDoesNotStopTheNext() {
        givenRecipients(
                customer(EMAIL, null),
                new NotificationRecipient(MERCHANT_USER, "MERCHANT", null, null));
        givenSaveReturnsTheRow();
        ParcelStatusChangedEvent event = event("PICKED_UP", "IN_TRANSIT");
        doThrow(new MailSendException("smtp down")).when(emailNotifier).send(EMAIL, event);

        service.handle(event);

        List<NotificationLog> rows = savedRows(3);
        assertThat(rows).filteredOn(row -> MERCHANT_USER.equals(row.getRecipientUserId()))
                .singleElement()
                .satisfies(row -> assertThat(row.getStatus()).isEqualTo(NotificationStatus.SENT));
    }

    // ----- helpers -----

    private void givenRecipients(NotificationRecipient... targets) {
        when(recipients.resolve(any())).thenReturn(List.of(targets));
    }

    private static NotificationRecipient customer(String email, String phoneNumber) {
        return new NotificationRecipient(CUSTOMER_USER, "CUSTOMER", email, phoneNumber);
    }

    private void givenSaveReturnsTheRow() {
        when(logs.saveAndFlush(any(NotificationLog.class))).thenAnswer(call -> call.getArgument(0));
    }

    private List<NotificationLog> savedRows(int expected) {
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.captor();
        verify(logs, Mockito.times(expected)).save(captor.capture());
        return captor.getAllValues();
    }

    private static NotificationLog rowOfType(List<NotificationLog> rows, NotificationType type) {
        return rows.stream().filter(row -> row.getType() == type).findFirst().orElseThrow();
    }

    private static ParcelStatusChangedEvent event(String oldStatus, String newStatus) {
        return new ParcelStatusChangedEvent(EVENT_ID, 42L, "SD7K2M9QX4LP", oldStatus, newStatus,
                Instant.parse("2026-11-14T10:15:30Z"), "COURIER", CUSTOMER_USER, 99L, "On the way");
    }

    private static ParcelStatusChangedEvent failureEvent(String reason) {
        return new ParcelStatusChangedEvent(EVENT_ID, 42L, "SD7K2M9QX4LP", "OUT_FOR_DELIVERY", "FAILED",
                Instant.parse("2026-11-14T10:15:30Z"), "COURIER", CUSTOMER_USER, 99L, "Nobody home", reason);
    }
}
