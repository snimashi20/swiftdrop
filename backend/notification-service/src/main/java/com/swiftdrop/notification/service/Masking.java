package com.swiftdrop.notification.service;

public final class Masking {

    private Masking() {}

    /** {@code sithmi@gmail.com} becomes {@code s***@gmail.com}. */
    public static String email(String email) {
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    /** {@code 0771234567} becomes {@code *******567}. */
    public static String phone(String phone) {
        String digits = phone.replaceAll("\\s", "");
        if (digits.length() <= 3) {
            return "***";
        }
        return "*".repeat(digits.length() - 3) + digits.substring(digits.length() - 3);
    }
}
