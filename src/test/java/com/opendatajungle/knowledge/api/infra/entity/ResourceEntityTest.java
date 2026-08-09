package com.opendatajungle.knowledge.api.infra.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceEntityTest {

    @Test
    void onCreate_shouldSetCreatedAtAndUpdatedAt_whenCreatedAtIsNull() {
        // Given
        ResourceEntity entity = new ResourceEntity();

        // When
        entity.onCreate();

        // Then
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isAfterOrEqualTo(entity.getCreatedAt());
    }

    @Test
    void onUpdate_shouldRefreshUpdatedAt_withoutTouchingCreatedAt() {
        // Given
        ResourceEntity entity = new ResourceEntity();
        LocalDateTime existingCreatedAt = LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0);
        LocalDateTime staleUpdatedAt = LocalDateTime.of(2020, Month.JANUARY, 2, 0, 0);
        entity.setCreatedAt(existingCreatedAt);
        entity.setUpdatedAt(staleUpdatedAt);

        // When
        entity.onUpdate();

        // Then
        assertThat(entity.getCreatedAt()).isEqualTo(existingCreatedAt);
        assertThat(entity.getUpdatedAt()).isNotNull().isNotEqualTo(staleUpdatedAt);
    }
}
