package com.swiftdrop.notification.service;

import com.swiftdrop.notification.common.NotFoundException;
import com.swiftdrop.notification.domain.NotificationLog;
import com.swiftdrop.notification.domain.NotificationStatus;
import com.swiftdrop.notification.domain.NotificationType;
import com.swiftdrop.notification.dto.NotificationLogResponse;
import com.swiftdrop.notification.dto.NotificationResponse;
import com.swiftdrop.notification.dto.PageResponse;
import com.swiftdrop.notification.repository.NotificationLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The read side of the API: feed grouping, unread counts, read marking and the admin filters. */
@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceTest {

    private static final Long CUSTOMER_ID = 7L;
    private static final UUID EVENT_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Mock NotificationLogRepository logs;

    @InjectMocks NotificationQueryService service;

    // ----- recipient feed -----

    @Test
    @DisplayName("the full feed reads the recipient query with an unsorted page and a readable message")
    void fullFeedUsesTheRecipientQuery() {
        when(logs.findRecipientFeed(eq(CUSTOMER_ID), any(Pageable.class)))
                .thenReturn(pageOf(row("IN_TRANSIT", null)));

        PageResponse<NotificationResponse> result = service.feed(CUSTOMER_ID, 0, 20, false);

        verify(logs, never()).findUnreadRecipientFeed(any(), any());
        NotificationResponse item = result.content().get(0);
        assertThat(item.id()).isEqualTo(5L);
        assertThat(item.parcelId()).isEqualTo(42L);
        assertThat(item.trackingNumber()).isEqualTo("SD7K2M9QX4LP");
        assertThat(item.oldStatus()).isEqualTo("PICKED_UP");
        assertThat(item.newStatus()).isEqualTo("IN_TRANSIT");
        assertThat(item.message()).isEqualTo("Parcel SD7K2M9QX4LP is now in transit.");
        assertThat(item.read()).isFalse();
        assertThat(item.readAt()).isNull();
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);

        // The feed query carries its own ORDER BY, so the Pageable must stay unsorted.
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.captor();
        verify(logs).findRecipientFeed(eq(CUSTOMER_ID), pageable.capture());
        assertThat(pageable.getValue().getSort().isSorted()).isFalse();
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("unreadOnly reads the unread query instead, and a read row reports its readAt")
    void unreadOnlyFeedUsesTheUnreadQuery() {
        Instant readAt = Instant.parse("2026-11-14T11:00:00Z");
        when(logs.findUnreadRecipientFeed(eq(CUSTOMER_ID), any(Pageable.class)))
                .thenReturn(pageOf(row("DELIVERED", readAt)));

        PageResponse<NotificationResponse> result = service.feed(CUSTOMER_ID, 2, 5, true);

        verify(logs, never()).findRecipientFeed(any(), any());
        assertThat(result.content().get(0).read()).isTrue();
        assertThat(result.content().get(0).readAt()).isEqualTo(readAt);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.captor();
        verify(logs).findUnreadRecipientFeed(eq(CUSTOMER_ID), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    @DisplayName("an underscored status reads as words in the message")
    void underscoredStatusIsMadeReadable() {
        when(logs.findRecipientFeed(eq(CUSTOMER_ID), any(Pageable.class)))
                .thenReturn(pageOf(row("OUT_FOR_DELIVERY", null)));

        assertThat(service.feed(CUSTOMER_ID, 0, 20, false).content().get(0).message())
                .isEqualTo("Parcel SD7K2M9QX4LP is now out for delivery.");
    }

    @Test
    @DisplayName("a cancellation reads as cancelled in the message")
    void cancelledStatusIsMadeReadable() {
        when(logs.findRecipientFeed(eq(CUSTOMER_ID), any(Pageable.class)))
                .thenReturn(pageOf(row("CANCELLED", null)));

        assertThat(service.feed(CUSTOMER_ID, 0, 20, false).content().get(0).message())
                .isEqualTo("Parcel SD7K2M9QX4LP is now cancelled.");
    }

    @Test
    @DisplayName("a row with no status falls back to updated instead of printing null")
    void missingStatusFallsBackToUpdated() {
        when(logs.findRecipientFeed(eq(CUSTOMER_ID), any(Pageable.class)))
                .thenReturn(pageOf(row(null, null)));

        assertThat(service.feed(CUSTOMER_ID, 0, 20, false).content().get(0).message())
                .isEqualTo("Parcel SD7K2M9QX4LP is now updated.");
    }

    @Test
    @DisplayName("an empty feed is an empty page, not a failure")
    void emptyFeedIsAnEmptyPage() {
        when(logs.findRecipientFeed(eq(CUSTOMER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        PageResponse<NotificationResponse> result = service.feed(CUSTOMER_ID, 0, 20, false);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.last()).isTrue();
    }

    // ----- per-role wording -----

    @Test
    @DisplayName("a merchant reads the same change as a shipment of theirs")
    void merchantMessageTalksAboutTheShipment() {
        when(logs.findRecipientFeed(eq(CUSTOMER_ID), any(Pageable.class)))
                .thenReturn(pageOf(rowFor("MERCHANT", "OUT_FOR_DELIVERY", null)));

        assertThat(service.feed(CUSTOMER_ID, 0, 20, false).content().get(0).message())
                .isEqualTo("Shipment SD7K2M9QX4LP is now out for delivery.");
    }

    @Test
    @DisplayName("a courier is told the parcel is one assigned to them")
    void courierMessageNamesTheirOwnAssignment() {
        when(logs.findRecipientFeed(eq(CUSTOMER_ID), any(Pageable.class)))
                .thenReturn(pageOf(rowFor("COURIER", "IN_TRANSIT", null)));

        assertThat(service.feed(CUSTOMER_ID, 0, 20, false).content().get(0).message())
                .isEqualTo("Parcel SD7K2M9QX4LP assigned to you is now in transit.");
    }

    @Test
    @DisplayName("a dispatcher is told what the booking needs, not just its status")
    void dispatcherMessageNamesTheActionForANewBooking() {
        when(logs.findRecipientFeed(eq(CUSTOMER_ID), any(Pageable.class)))
                .thenReturn(pageOf(rowFor("DISPATCHER", "CREATED", null)));

        assertThat(service.feed(CUSTOMER_ID, 0, 20, false).content().get(0).message())
                .isEqualTo("Parcel SD7K2M9QX4LP was booked and is waiting to be assigned.");
    }

    @Test
    @DisplayName("an admin is told a failed delivery needs re-dispatch")
    void adminMessageNamesTheActionForAFailure() {
        when(logs.findRecipientFeed(eq(CUSTOMER_ID), any(Pageable.class)))
                .thenReturn(pageOf(rowFor("ADMIN", "FAILED", null)));

        assertThat(service.feed(CUSTOMER_ID, 0, 20, false).content().get(0).message())
                .isEqualTo("Delivery of parcel SD7K2M9QX4LP failed and needs re-dispatch.");
    }

    @Test
    @DisplayName("staff see a cancellation stated plainly")
    void staffMessageForACancellation() {
        when(logs.findRecipientFeed(eq(CUSTOMER_ID), any(Pageable.class)))
                .thenReturn(pageOf(rowFor("DISPATCHER", "CANCELLED", null)));

        assertThat(service.feed(CUSTOMER_ID, 0, 20, false).content().get(0).message())
                .isEqualTo("Parcel SD7K2M9QX4LP was cancelled.");
    }

    @Test
    @DisplayName("a status staff are not normally notified about still reads sensibly for them")
    void staffMessageFallsBackToThePlainStatusLine() {
        when(logs.findRecipientFeed(eq(CUSTOMER_ID), any(Pageable.class)))
                .thenReturn(pageOf(rowFor("ADMIN", "DELIVERED", null)));

        assertThat(service.feed(CUSTOMER_ID, 0, 20, false).content().get(0).message())
                .isEqualTo("Parcel SD7K2M9QX4LP is now delivered.");
    }

    @Test
    @DisplayName("a legacy row written before recipients existed is worded as the customer's")
    void missingRecipientRoleReadsAsTheCustomerWording() {
        when(logs.findRecipientFeed(eq(CUSTOMER_ID), any(Pageable.class)))
                .thenReturn(pageOf(rowFor(null, "DELIVERED", null)));

        assertThat(service.feed(CUSTOMER_ID, 0, 20, false).content().get(0).message())
                .isEqualTo("Parcel SD7K2M9QX4LP is now delivered.");
    }

    // ----- failed deliveries name the reason -----

    @Test
    @DisplayName("a customer is told the delivery could not be completed, and why")
    void customerFailureNamesTheReason() {
        when(logs.findRecipientFeed(eq(CUSTOMER_ID), any(Pageable.class)))
                .thenReturn(pageOf(failureRow("CUSTOMER", "RECIPIENT_NOT_AVAILABLE")));

        assertThat(service.feed(CUSTOMER_ID, 0, 20, false).content().get(0).message())
                .isEqualTo("Delivery of parcel SD7K2M9QX4LP could not be completed."
                        + " Nobody was available to receive the parcel.");
    }

    @Test
    @DisplayName("a dispatcher gets the reason appended to the re-dispatch line")
    void staffFailureNamesTheReason() {
        when(logs.findRecipientFeed(eq(CUSTOMER_ID), any(Pageable.class)))
                .thenReturn(pageOf(failureRow("DISPATCHER", "WRONG_ADDRESS")));

        assertThat(service.feed(CUSTOMER_ID, 0, 20, false).content().get(0).message())
                .isEqualTo("Delivery of parcel SD7K2M9QX4LP failed and needs re-dispatch."
                        + " The delivery address could not be found.");
    }

    @Test
    @DisplayName("a failure with no reason recorded reads as a complete sentence, with nothing dangling")
    void failureWithoutAReasonStillReadsCleanly() {
        when(logs.findRecipientFeed(eq(CUSTOMER_ID), any(Pageable.class)))
                .thenReturn(pageOf(failureRow("CUSTOMER", null)));

        assertThat(service.feed(CUSTOMER_ID, 0, 20, false).content().get(0).message())
                .isEqualTo("Delivery of parcel SD7K2M9QX4LP could not be completed.");
    }

    @Test
    @DisplayName("a reason this service has no wording for is still rendered readably")
    void unknownReasonFallsBackToReadableText() {
        when(logs.findRecipientFeed(eq(CUSTOMER_ID), any(Pageable.class)))
                .thenReturn(pageOf(failureRow("CUSTOMER", "VEHICLE_BREAKDOWN")));

        assertThat(service.feed(CUSTOMER_ID, 0, 20, false).content().get(0).message())
                .isEqualTo("Delivery of parcel SD7K2M9QX4LP could not be completed."
                        + " Vehicle breakdown.");
    }

    @Test
    @DisplayName("a merchant sees the failure wording too, not the shipment line")
    void merchantFailureUsesTheFailureWording() {
        when(logs.findRecipientFeed(eq(CUSTOMER_ID), any(Pageable.class)))
                .thenReturn(pageOf(failureRow("MERCHANT", "REFUSED")));

        assertThat(service.feed(CUSTOMER_ID, 0, 20, false).content().get(0).message())
                .isEqualTo("Delivery of parcel SD7K2M9QX4LP could not be completed."
                        + " The recipient refused the parcel.");
    }

    // ----- unread count and read marking -----

    @Test
    @DisplayName("the unread count comes straight from the status-change count")
    void unreadCountCountsStatusChanges() {
        when(logs.countUnreadEvents(CUSTOMER_ID)).thenReturn(3L);

        assertThat(service.unreadCount(CUSTOMER_ID).unreadCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("read-all reports how many rows it touched, even when that is none")
    void markAllReadReportsTheRowCount() {
        when(logs.markAllRead(eq(CUSTOMER_ID), any(Instant.class))).thenReturn(4);

        assertThat(service.markAllRead(CUSTOMER_ID).updated()).isEqualTo(4);

        ArgumentCaptor<Instant> now = ArgumentCaptor.captor();
        verify(logs).markAllRead(eq(CUSTOMER_ID), now.capture());
        assertThat(now.getValue()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("read-all on an already-read inbox reports zero")
    void markAllReadOnAnEmptyInboxReportsZero() {
        when(logs.markAllRead(eq(CUSTOMER_ID), any(Instant.class))).thenReturn(0);

        assertThat(service.markAllRead(CUSTOMER_ID).updated()).isZero();
    }

    @Test
    @DisplayName("marking one notification read marks every channel row of that status change")
    void markReadCoversEveryChannelOfTheEvent() {
        when(logs.findByIdAndRecipientUserId(5L, CUSTOMER_ID)).thenReturn(Optional.of(row("IN_TRANSIT", null)));
        when(logs.markEventRead(eq(EVENT_ID), eq(CUSTOMER_ID), any(Instant.class))).thenReturn(2);

        assertThat(service.markRead(5L, CUSTOMER_ID).updated()).isEqualTo(2);
    }

    @Test
    @DisplayName("a notification that does not exist for this recipient is a 404, and nothing is updated")
    void markReadOnSomeoneElsesNotificationIsNotFound() {
        when(logs.findByIdAndRecipientUserId(5L, CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(5L, CUSTOMER_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("No notification 5 for this account.");

        verify(logs, never()).markEventRead(any(), any(), any());
    }

    // ----- admin log -----

    @Test
    @DisplayName("the admin log filters by type, status, customer and a normalised tracking number")
    void adminLogBuildsAProbeFromEveryFilter() {
        when(logs.findAll(ArgumentMatchers.<Example<NotificationLog>>any(), any(Pageable.class)))
                .thenReturn(pageOf(row("IN_TRANSIT", null)));

        PageResponse<NotificationLogResponse> result = service.adminLog(NotificationType.EMAIL,
                NotificationStatus.FAILED, "  sd7k2m9qx4lp  ", CUSTOMER_ID, 1, 50);

        ArgumentCaptor<Example<NotificationLog>> example = ArgumentCaptor.captor();
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.captor();
        verify(logs).findAll(example.capture(), pageable.capture());

        NotificationLog probe = example.getValue().getProbe();
        assertThat(probe.getType()).isEqualTo(NotificationType.EMAIL);
        assertThat(probe.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(probe.getTrackingNumber()).isEqualTo("SD7K2M9QX4LP");
        assertThat(probe.getCustomerId()).isEqualTo(CUSTOMER_ID);

        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
        assertThat(pageable.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt", "id"));

        NotificationLogResponse item = result.content().get(0);
        assertThat(item.eventId()).isEqualTo(EVENT_ID);
        assertThat(item.recipientUserId()).isEqualTo(CUSTOMER_ID);
        assertThat(item.recipientRole()).isEqualTo("CUSTOMER");
        assertThat(item.type()).isEqualTo(NotificationType.SMS);
        assertThat(item.recipient()).isEqualTo("*******567");
        assertThat(item.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(item.failureReason()).isNull();
        assertThat(item.sentAt()).isEqualTo(Instant.parse("2026-11-14T10:15:31Z"));
    }

    @Test
    @DisplayName("no filters at all leaves every probe field null, so the log is unfiltered")
    void adminLogWithoutFiltersLeavesTheProbeEmpty() {
        when(logs.findAll(ArgumentMatchers.<Example<NotificationLog>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        service.adminLog(null, null, null, null, 0, 20);

        ArgumentCaptor<Example<NotificationLog>> example = ArgumentCaptor.captor();
        verify(logs).findAll(example.capture(), any(Pageable.class));
        NotificationLog probe = example.getValue().getProbe();
        assertThat(probe.getType()).isNull();
        assertThat(probe.getStatus()).isNull();
        assertThat(probe.getTrackingNumber()).isNull();
        assertThat(probe.getCustomerId()).isNull();
    }

    @Test
    @DisplayName("a blank tracking number is treated as no tracking-number filter")
    void adminLogIgnoresABlankTrackingNumber() {
        when(logs.findAll(ArgumentMatchers.<Example<NotificationLog>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        service.adminLog(null, null, "   ", null, 0, 20);

        ArgumentCaptor<Example<NotificationLog>> example = ArgumentCaptor.captor();
        verify(logs).findAll(example.capture(), any(Pageable.class));
        assertThat(example.getValue().getProbe().getTrackingNumber()).isNull();
    }

    // ----- helpers -----

    private static Page<NotificationLog> pageOf(NotificationLog row) {
        return new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1);
    }

    /** One log row as the consumer writes it: an SMS row for the customer, recipient masked. */
    private static NotificationLog row(String newStatus, Instant readAt) {
        return rowFor("CUSTOMER", newStatus, readAt);
    }

    /** A FAILED row as the consumer writes it, carrying the parcel's own failure reason. */
    private static NotificationLog failureRow(String recipientRole, String parcelFailureReason) {
        NotificationLog row = rowFor(recipientRole, "FAILED", null);
        row.setParcelFailureReason(parcelFailureReason);
        return row;
    }

    /** The same row written for another role, which is what changes the wording of the message. */
    private static NotificationLog rowFor(String recipientRole, String newStatus, Instant readAt) {
        return NotificationLog.builder()
                .id(5L)
                .eventId(EVENT_ID)
                .parcelId(42L)
                .trackingNumber("SD7K2M9QX4LP")
                .oldStatus("PICKED_UP")
                .newStatus(newStatus)
                .customerId(CUSTOMER_ID)
                .recipientUserId(CUSTOMER_ID)
                .recipientRole(recipientRole)
                .type(NotificationType.SMS)
                .recipient("*******567")
                .status(NotificationStatus.SENT)
                .createdAt(Instant.parse("2026-11-14T10:15:30Z"))
                .sentAt(Instant.parse("2026-11-14T10:15:31Z"))
                .readAt(readAt)
                .build();
    }
}
