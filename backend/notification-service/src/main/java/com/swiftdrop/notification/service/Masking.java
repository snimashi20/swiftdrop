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

    /**
     * Masks every e-mail address inside free text, for log lines that quote a gateway's own words.
     * A provider happily echoes the address back in its error, and that must not reach the log in
     * full just because it arrived inside a sentence.
     */
    public static String text(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return EMAIL_IN_TEXT.matcher(value).replaceAll(match -> email(match.group()));
    }

    private static final java.util.regex.Pattern EMAIL_IN_TEXT =
            java.util.regex.Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+");

    /** {@code 0771234567} becomes {@code *******567}. */
    public static String phone(String phone) {
        String digits = phone.replaceAll("\\s", "");
        if (digits.length() <= 3) {
            return "***";
        }
        return "*".repeat(digits.length() - 3) + digits.substring(digits.length() - 3);
    }
}
