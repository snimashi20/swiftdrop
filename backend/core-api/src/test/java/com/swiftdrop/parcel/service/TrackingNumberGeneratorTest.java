package com.swiftdrop.parcel.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackingNumberGeneratorTest {

    private final TrackingNumberGenerator generator = new TrackingNumberGenerator();

    @Test
    void generatesSdPlusTenUppercaseLettersOrDigits() {
        String number = generator.generate();
        assertEquals(12, number.length());
        assertTrue(number.matches("SD[A-Z0-9]{10}"));
    }

    @Test
    void generatesDifferentNumbers() {
        assertNotEquals(generator.generate(), generator.generate());
    }
}