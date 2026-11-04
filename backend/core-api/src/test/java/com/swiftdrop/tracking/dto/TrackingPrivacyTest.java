package com.swiftdrop.tracking.dto;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards the "personal data hidden" rule: the public tracking response and the WebSocket
 * message may only ever contain these fields. If someone adds a phone, address or note,
 * this test fails and forces a conscious decision.
 */
class TrackingPrivacyTest {

    private Set<String> fieldsOf(Class<?> recordType) {
        return Arrays.stream(recordType.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());
    }

    @Test
    void publicTrackingResponseHasOnlySafeFields() {
        assertEquals(
                Set.of("trackingNumber", "status", "receiverName", "pickupCity", "dropoffCity", "timeline"),
                fieldsOf(TrackingResponse.class));
        assertEquals(Set.of("status", "changedAt"), fieldsOf(TrackingResponse.Entry.class));
    }

    @Test
    void webSocketMessageHasOnlyTrackingNumberStatusAndTime() {
        assertEquals(Set.of("trackingNumber", "status", "changedAt"), fieldsOf(TrackingUpdateMessage.class));
    }
}
