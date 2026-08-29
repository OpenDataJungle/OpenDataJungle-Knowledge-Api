package com.opendatajungle.knowledge.api.infra.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

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
        Instant existingCreatedAt = Instant.parse("2020-01-01T00:00:00Z");
        Instant staleUpdatedAt = Instant.parse("2020-01-02T00:00:00Z");
        entity.setCreatedAt(existingCreatedAt);
        entity.setUpdatedAt(staleUpdatedAt);

        // When
        entity.onUpdate();

        // Then
        assertThat(entity.getCreatedAt()).isEqualTo(existingCreatedAt);
        assertThat(entity.getUpdatedAt()).isNotNull().isNotEqualTo(staleUpdatedAt);
    }
}
