package com.swiftdrop.notification.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaskingTest {

    @Test
    void masksEmailKeepingFirstLetterAndDomain() {
        assertThat(Masking.email("sithmi@gmail.com")).isEqualTo("s***@gmail.com");
        assertThat(Masking.email("not-an-email")).isEqualTo("***");
    }

    @Test
    void masksPhoneKeepingLastThreeDigits() {
        assertThat(Masking.phone("0771234567")).isEqualTo("*******567");
        assertThat(Masking.phone("+94771234567")).isEqualTo("*********567");
        assertThat(Masking.phone("12")).isEqualTo("***");
    }
}
