package com.swiftdrop.notification.service;

import com.swiftdrop.notification.event.ParcelStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Works out everyone a status change has to reach. Read-only queries against core-api's tables in
 * the same database, because the event carries ids and nothing else.
 *
 * <p>The customer is mandatory: an event naming a customer who does not exist is a data integrity
 * problem, so it is rejected to the dead-letter queue rather than half-delivered. Every other
 * recipient is best effort — an unassigned parcel simply has no courier to notify, and a parcel
 * booked by a private customer has no merchant.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecipientDirectory {

    private final JdbcTemplate jdbc;
    private final CustomerDirectory customers;

    /**
     * The recipients of one event, customer first, de-duplicated by user id so that a staff user who
     * also happens to be the parcel's customer is notified once.
     *
     * @throws AmqpRejectAndDontRequeueException if the event's customer does not exist
     */
    public List<NotificationRecipient> resolve(ParcelStatusChangedEvent event) {
        Map<Long, NotificationRecipient> byUser = new LinkedHashMap<>();

        put(byUser, customer(event));
        put(byUser, merchantOwner(event.merchantId()));
        put(byUser, assignedCourier(event.parcelId()));
        if (NotificationPolicy.staffWatch(event.newStatus())) {
            staff().forEach(recipient -> put(byUser, Optional.of(recipient)));
        }

        return new ArrayList<>(byUser.values());
    }

    private static void put(Map<Long, NotificationRecipient> byUser, Optional<NotificationRecipient> recipient) {
        recipient.ifPresent(r -> byUser.putIfAbsent(r.userId(), r));
    }

    private Optional<NotificationRecipient> customer(ParcelStatusChangedEvent event) {
        return Optional.of(customers.findById(event.customerId())
                .map(contact -> new NotificationRecipient(event.customerId(), NotificationRecipient.CUSTOMER,
                        contact.email(), contact.phoneNumber()))
                .orElseThrow(() -> new AmqpRejectAndDontRequeueException(
                        "No user with id " + event.customerId() + " for event " + event.eventId())));
    }

    /** The user who owns the merchant account the parcel was booked under, if there is one. */
    private Optional<NotificationRecipient> merchantOwner(Long merchantId) {
        if (merchantId == null) {
            return Optional.empty();
        }
        return jdbc.query("""
                        SELECT u.id, u.role, u.email, u.phone_number
                          FROM merchants m
                          JOIN users u ON u.id = m.user_id
                         WHERE m.id = ? AND u.enabled = TRUE
                        """,
                (rs, i) -> new NotificationRecipient(rs.getLong("id"), NotificationRecipient.MERCHANT,
                        rs.getString("email"), rs.getString("phone_number")),
                merchantId
        ).stream().findFirst();
    }

    /** The courier the parcel is assigned to right now, if it has been assigned. */
    private Optional<NotificationRecipient> assignedCourier(Long parcelId) {
        if (parcelId == null) {
            return Optional.empty();
        }
        return jdbc.query("""
                        SELECT u.id, u.email, u.phone_number
                          FROM parcels p
                          JOIN users u ON u.id = p.assigned_courier_id
                         WHERE p.id = ? AND u.enabled = TRUE
                        """,
                (rs, i) -> new NotificationRecipient(rs.getLong("id"), NotificationRecipient.COURIER,
                        rs.getString("email"), rs.getString("phone_number")),
                parcelId
        ).stream().findFirst();
    }

    /** Every active dispatcher and admin. Small by nature: these are staff accounts, not customers. */
    private List<NotificationRecipient> staff() {
        return jdbc.query("""
                        SELECT id, role, email, phone_number
                          FROM users
                         WHERE role IN ('DISPATCHER', 'ADMIN')
                           AND enabled = TRUE
                           AND status = 'ACTIVE'
                         ORDER BY id
                        """,
                (rs, i) -> new NotificationRecipient(rs.getLong("id"), rs.getString("role"),
                        rs.getString("email"), rs.getString("phone_number")));
    }
}
