package com.laulem.vectopath.shared.util;

import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class DateUtils {
    public static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
