package com.swiftdrop.notification.service;

import java.util.Locale;

/**
 * Turns the codes an event carries into the words a person reads.
 *
 * <p>Every channel goes through here so the e-mail subject, the SMS and the in-app feed describe
 * the same change the same way. The codes arrive as plain strings, not enums: core-api owns
 * {@code ParcelStatus} and {@code FailureReason}, and this service deliberately does not depend on
 * it. So every method falls back to a readable rendering of whatever it is given rather than
 * failing on a value added later.
 */
public final class StatusText {

    private StatusText() {
    }

    /** {@code OUT_FOR_DELIVERY} reads as {@code out for delivery}. */
    public static String readable(String status) {
        if (status == null || status.isBlank()) {
            return "updated";
        }
        return status.trim().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    /**
     * What a failure reason means to the customer, as a sentence.
     *
     * @return null when there is no reason to show, so callers can simply skip the line
     */
    public static String failureReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        return switch (reason.trim().toUpperCase(Locale.ROOT)) {
            case "RECIPIENT_NOT_AVAILABLE" -> "Nobody was available to receive the parcel.";
            case "WRONG_ADDRESS" -> "The delivery address could not be found.";
            case "REFUSED" -> "The recipient refused the parcel.";
            case "DAMAGED" -> "The parcel was damaged.";
            case "OTHER" -> "The courier could not complete the delivery.";
            // A reason added in core-api after this was written still reads sensibly.
            default -> capitalise(readable(reason)) + ".";
        };
    }

    /** A short reason for the SMS, which has no room for a sentence. */
    public static String shortFailureReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        return readable(reason);
    }

    private static String capitalise(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
