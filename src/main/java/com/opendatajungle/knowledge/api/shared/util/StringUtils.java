package com.opendatajungle.knowledge.api.shared.util;

import java.util.regex.Pattern;

public final class StringUtils {
    private static final int MAX_LOG_VALUE_LENGTH = 20000;
    private static final Pattern CONTROL_CHAR_PATTERN = Pattern.compile("[\\p{Cntrl}]");

    private StringUtils() {
    }

    public static boolean isNullOrBlank(String value) {
        return value == null || value.isBlank();
    }

    public static boolean hasText(String value) {
        return !isNullOrBlank(value);
    }

    /**
     * Neutralizes user-controlled values before logging them:
     * strips CR/LF and other control characters (log forging) and caps the length.
     */
    public static String sanitizeForLog(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = CONTROL_CHAR_PATTERN.matcher(value).replaceAll("_");
        return sanitized.length() <= MAX_LOG_VALUE_LENGTH
                ? sanitized
                : sanitized.substring(0, MAX_LOG_VALUE_LENGTH) + "...";
    }
}
