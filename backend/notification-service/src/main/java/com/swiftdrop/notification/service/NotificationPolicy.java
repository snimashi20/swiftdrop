package com.swiftdrop.notification.service;

import com.swiftdrop.notification.domain.NotificationType;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Who hears about a status change, and on which channels.
 *
 * <p>Both rules live here on purpose: they are the two things most likely to be tuned, and keeping
 * them in one place means neither the consumer nor the directory has to be touched to change them.
 *
 * <h2>Channels</h2>
 * Only the customer gets e-mail and SMS. The merchant, the courier and the dispatch/admin staff are
 * <strong>in-app only</strong>: they work inside SwiftDrop all day, so a mailbox copy of every
 * status change would be noise, and for staff it would be one mail per parcel per transition.
 *
 * <h2>Which statuses</h2>
 * The customer, the merchant and the assigned courier hear about every transition of a parcel that
 * is theirs. Staff hear only about the transitions that need someone to act — a new booking waiting
 * to be assigned, a failed delivery, a cancellation — because a dispatcher watching every
 * {@code IN_TRANSIT} of every parcel in the country would see nothing at all.
 */
public final class NotificationPolicy {

    /** The only role that is also reachable off-platform. */
    private static final List<NotificationType> CUSTOMER_CHANNELS =
            List.of(NotificationType.IN_APP, NotificationType.EMAIL, NotificationType.SMS);

    /** Merchant, courier, dispatcher, admin: inside the app only, no mailbox and no SMS. */
    private static final List<NotificationType> IN_APP_ONLY = List.of(NotificationType.IN_APP);

    /** The transitions a dispatcher or an admin is notified about. */
    private static final Set<String> STAFF_WATCHED_STATUSES = Set.of("CREATED", "FAILED", "CANCELLED");

    private NotificationPolicy() {
    }

    /** The channels a recipient in this role is notified on, in the order they are attempted. */
    public static List<NotificationType> channelsFor(String role) {
        return NotificationRecipient.CUSTOMER.equalsIgnoreCase(role) ? CUSTOMER_CHANNELS : IN_APP_ONLY;
    }

    /** Whether dispatchers and admins should be told about this transition at all. */
    public static boolean staffWatch(String newStatus) {
        return newStatus != null && STAFF_WATCHED_STATUSES.contains(newStatus.toUpperCase(Locale.ROOT));
    }
}
