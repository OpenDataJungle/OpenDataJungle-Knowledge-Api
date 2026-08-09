package com.opendatajungle.knowledge.api.shared.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringUtilsTest {

    @Test
    void isNullOrBlank_shouldReturnTrue_whenValueIsNullOrBlank() {
        assertThat(StringUtils.isNullOrBlank(null)).isTrue();
        assertThat(StringUtils.isNullOrBlank("")).isTrue();
        assertThat(StringUtils.isNullOrBlank("   ")).isTrue();
    }

    @Test
    void isNullOrBlank_shouldReturnFalse_whenValueHasText() {
        assertThat(StringUtils.isNullOrBlank("value")).isFalse();
    }

    @Test
    void hasText_shouldReturnTrue_whenValueHasText() {
        assertThat(StringUtils.hasText("value")).isTrue();
    }

    @Test
    void hasText_shouldReturnFalse_whenValueIsNullOrBlank() {
        assertThat(StringUtils.hasText(null)).isFalse();
        assertThat(StringUtils.hasText(" ")).isFalse();
    }

    @Test
    void sanitizeForLog_shouldReturnNull_whenValueIsNull() {
        assertThat(StringUtils.sanitizeForLog(null)).isNull();
    }

    @Test
    void sanitizeForLog_shouldReturnValueUnchanged_whenNoControlCharacters() {
        assertThat(StringUtils.sanitizeForLog("clean value")).isEqualTo("clean value");
    }

    @Test
    void sanitizeForLog_shouldReplaceCarriageReturnAndLineFeed_withUnderscore() {
        assertThat(StringUtils.sanitizeForLog("line1\r\nline2")).isEqualTo("line1__line2");
    }

    @Test
    void sanitizeForLog_shouldReplaceTabAndOtherControlCharacters_withUnderscore() {
        String withControlChars = "a" + (char) 0x09 + "b" + (char) 0x07 + "c";

        assertThat(StringUtils.sanitizeForLog(withControlChars)).isEqualTo("a_b_c");
    }

    @Test
    void sanitizeForLog_shouldReturnValueUnchanged_whenLengthEqualsMaxAllowedLength() {
        // Given
        String value = "a".repeat(20000);

        // When
        String result = StringUtils.sanitizeForLog(value);

        // Then
        assertThat(result).isEqualTo(value);
        assertThat(result).hasSize(20000);
    }

    @Test
    void sanitizeForLog_shouldTruncateAndAppendEllipsis_whenValueExceedsMaxAllowedLength() {
        // Given
        String value = "a".repeat(20100);

        // When
        String result = StringUtils.sanitizeForLog(value);

        // Then
        assertThat(result).hasSize(20003);
        assertThat(result).isEqualTo("a".repeat(20000) + "...");
    }
}
