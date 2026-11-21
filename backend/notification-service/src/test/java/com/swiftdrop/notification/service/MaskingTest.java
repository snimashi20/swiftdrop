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
    void masksEveryAddressInsideFreeText() {
        // The shape of a real Gmail rejection, which quotes the address straight back.
        assertThat(Masking.text("535-5.7.8 Username and Password not accepted for sithmi@gmail.com h5-2"))
                .isEqualTo("535-5.7.8 Username and Password not accepted for s***@gmail.com h5-2");
        assertThat(Masking.text("relay denied: a@x.com and bob.smith+tag@mail.co.uk"))
                .isEqualTo("relay denied: a***@x.com and b***@mail.co.uk");
    }

    @Test
    void textWithNoAddressIsLeftAlone() {
        assertThat(Masking.text("Connection timed out")).isEqualTo("Connection timed out");
        assertThat(Masking.text(null)).isNull();
        assertThat(Masking.text("")).isEmpty();
    }

    @Test
    void masksPhoneKeepingLastThreeDigits() {
        assertThat(Masking.phone("0771234567")).isEqualTo("*******567");
        assertThat(Masking.phone("+94771234567")).isEqualTo("*********567");
        assertThat(Masking.phone("12")).isEqualTo("***");
    }
}
