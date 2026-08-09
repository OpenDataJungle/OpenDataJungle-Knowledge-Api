package com.opendatajungle.knowledge.api.client.dto;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CreateResourceRequestTest {

    @Test
    void groupPermissions_shouldReturnEmptyList_whenConstructedWithNull() {
        // Given
        CreateResourceRequest request = new CreateResourceRequest("name", "content", null, "TEXT", null, null, null);

        // When
        List<ResourceGroupPermissionRequest> groupPermissions = request.groupPermissions();

        // Then
        assertThat(groupPermissions).isEmpty();
    }

    @Test
    void groupPermissions_shouldReturnProvidedList_whenNotNull() {
        // Given
        ResourceGroupPermissionRequest permission = new ResourceGroupPermissionRequest(UUID.randomUUID(), UUID.randomUUID());
        CreateResourceRequest request = new CreateResourceRequest("name", "content", null, "TEXT", null, null, List.of(permission));

        // When
        List<ResourceGroupPermissionRequest> groupPermissions = request.groupPermissions();

        // Then
        assertThat(groupPermissions).containsExactly(permission);
    }
}
