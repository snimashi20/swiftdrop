package com.swiftdrop.tracking.service;

import com.swiftdrop.tracking.domain.ParcelStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusTransitionRulesTest {

    private final StatusTransitionRules rules = new StatusTransitionRules();

    @ParameterizedTest
    @CsvSource({
            "CREATED,PICKED_UP",
            "PICKED_UP,IN_TRANSIT",
            "PICKED_UP,FAILED",
            "IN_TRANSIT,OUT_FOR_DELIVERY",
            "IN_TRANSIT,FAILED",
            "OUT_FOR_DELIVERY,DELIVERED",
            "OUT_FOR_DELIVERY,FAILED",
            "FAILED,OUT_FOR_DELIVERY"
    })
    void allowsValidTransitions(ParcelStatus from, ParcelStatus to) {
        assertTrue(rules.canTransition(from, to));
        assertDoesNotThrow(() -> rules.validate(from, to));
    }

    @ParameterizedTest
    @CsvSource({
            "DELIVERED,IN_TRANSIT",
            "DELIVERED,FAILED",
            "DELIVERED,CREATED",
            "CREATED,DELIVERED",
            "CREATED,FAILED",
            "CREATED,CREATED",
            "IN_TRANSIT,PICKED_UP",
            "PICKED_UP,DELIVERED",
            "FAILED,DELIVERED"
    })
    void refusesInvalidTransitions(ParcelStatus from, ParcelStatus to) {
        assertFalse(rules.canTransition(from, to));
        assertThrows(InvalidStatusTransitionException.class, () -> rules.validate(from, to));
    }

    @Test
    void errorMessageNamesBothStatusesOnly() {
        InvalidStatusTransitionException ex = assertThrows(
                InvalidStatusTransitionException.class,
                () -> rules.validate(ParcelStatus.DELIVERED, ParcelStatus.IN_TRANSIT));
        assertEquals("Cannot change status from DELIVERED to IN_TRANSIT", ex.getMessage());
    }
}