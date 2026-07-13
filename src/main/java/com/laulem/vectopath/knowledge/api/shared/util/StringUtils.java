package com.laulem.vectopath.knowledge.api.shared.util;

public final class StringUtils {
    private StringUtils() {
    }

    public static boolean isNullOrBlank(String value) {
        return value == null || value.isBlank();
    }

    public static boolean hasText(String value) {
        return !isNullOrBlank(value);
    }
}
