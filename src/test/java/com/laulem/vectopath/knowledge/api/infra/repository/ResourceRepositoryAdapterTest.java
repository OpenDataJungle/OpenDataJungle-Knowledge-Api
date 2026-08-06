package com.laulem.vectopath.knowledge.api.infra.repository;

import com.laulem.vectopath.knowledge.api.business.exception.NotFoundException;
import com.laulem.vectopath.knowledge.api.business.model.Resource;
import com.laulem.vectopath.knowledge.api.business.model.ResourceGroupPermission;
import com.laulem.vectopath.knowledge.api.business.model.ResourceStatus;
import com.laulem.vectopath.knowledge.api.business.service.AuthenticationUseCase;
import com.laulem.vectopath.knowledge.api.infra.entity.ResourceEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceRepositoryAdapterTest {

    @Mock
    private ResourceJpaRepository jpaRepository;

    @Mock
    private ResourceGroupPermissionJpaRepository groupPermissionJpaRepository;

    @Mock
    private AuthenticationUseCase authenticationUseCase;

    @InjectMocks
    private ResourceRepositoryAdapter adapter;

    @Test
    void save_shouldPersistEntityAndReturnMappedResource() {
        // Given
        Resource resource = new Resource("doc", "content", "text/plain", null);
        ResourceEntity savedEntity = new ResourceEntity();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setName("doc");
        when(jpaRepository.saveAndFlush(any(ResourceEntity.class))).thenReturn(savedEntity);

        // When
        Resource result = adapter.save(resource);

        // Then
        assertThat(result.getId()).isEqualTo(savedEntity.getId());
        assertThat(result.getName()).isEqualTo(savedEntity.getName());
    }

    @Test
    void save_shouldAssignGroupPermissions_whenPresent() {
        // Given
        UUID groupId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();
        Resource resource = new Resource("doc", "content", "text/plain", null);
        resource.setGroupPermissions(List.of(new ResourceGroupPermission(groupId, permissionId)));
        ResourceEntity savedEntity = new ResourceEntity();
        savedEntity.setId(UUID.randomUUID());
        when(jpaRepository.saveAndFlush(any(ResourceEntity.class))).thenReturn(savedEntity);

        // When
        adapter.save(resource);

        // Then
        ArgumentCaptor<List<com.laulem.vectopath.knowledge.api.infra.entity.ResourceGroupPermissionEntity>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(groupPermissionJpaRepository).saveAllAndFlush(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().getFirst().getId().getResourceId()).isEqualTo(savedEntity.getId());
        assertThat(captor.getValue().getFirst().getId().getGroupId()).isEqualTo(groupId);
        assertThat(captor.getValue().getFirst().getPermissionId()).isEqualTo(permissionId);
    }

    @Test
    void save_shouldNotAssignGroupPermissions_whenNoneProvided() {
        // Given
        Resource resource = new Resource("doc", "content", "text/plain", null);
        when(jpaRepository.saveAndFlush(any(ResourceEntity.class))).thenReturn(new ResourceEntity());

        // When
        adapter.save(resource);

        // Then
        verify(groupPermissionJpaRepository, never()).saveAllAndFlush(anyList());
    }

    @Test
    void  findByIdWithAccessControl_shouldReturnMappedResource_whenFound() {
        // Given
        UUID id = UUID.randomUUID();
        ResourceEntity entity = new ResourceEntity();
        entity.setId(id);
        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");
        when(jpaRepository.findWithAccessControl(id.toString(), null, null, null, "alice"))
                .thenReturn(List.of(entity));

        // When
        Optional<Resource> result = adapter.findByIdWithAccessControl(id);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(id);
    }

    @Test
    void  findByIdWithAccessControl_shouldReturnEmpty_whenNotFound() {
        // Given
        UUID id = UUID.randomUUID();
        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");
        when(jpaRepository.findWithAccessControl(id.toString(), null, null, null, "alice"))
                .thenReturn(List.of());

        // When
        Optional<Resource> result = adapter.findByIdWithAccessControl(id);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findAllWithAccessControl_shouldReturnMappedResource() {
        // Given
        ResourceEntity entity = new ResourceEntity();
        entity.setId(UUID.randomUUID());
        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");
        when(jpaRepository.findWithAccessControl(isNull(), isNull(), isNull(), isNull(), eq("alice")))
                .thenReturn(List.of(entity));

        // When
        List<Resource> result = adapter.findAllWithAccessControl();

        // Then
        assertThat(result).extracting(Resource::getId).containsExactly(entity.getId());
    }

    @Test
    void findByStatusWithAccessControl_shouldReturnMappedResource() {
        // Given
        ResourceEntity entity = new ResourceEntity();
        entity.setId(UUID.randomUUID());
        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");
        when(jpaRepository.findWithAccessControl(isNull(), eq("VECTORIZED"), isNull(), isNull(), eq("alice")))
                .thenReturn(List.of(entity));

        // When
        List<Resource> result = adapter.findByStatusWithAccessControl(ResourceStatus.VECTORIZED);

        // Then
        assertThat(result).extracting(Resource::getId).containsExactly(entity.getId());
    }

    @Test
    void searchWithAccessControl_shouldReturnMappedResource() {
        // Given
        ResourceEntity entity = new ResourceEntity();
        entity.setId(UUID.randomUUID());
        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");
        when(jpaRepository.findWithAccessControl(isNull(), isNull(), eq("report"), eq("/docs"), eq("alice")))
                .thenReturn(List.of(entity));

        // When
        List<Resource> result = adapter.searchWithAccessControl("report", "/docs");

        // Then
        assertThat(result).extracting(Resource::getId).containsExactly(entity.getId());
    }

    @Test
    void deleteById_shouldDelegate() {
        // Given
        UUID id = UUID.randomUUID();

        // When
        adapter.deleteById(id);

        // Then
        verify(jpaRepository).deleteById(id);
    }

    @Test
    void updateStatus_shouldDelegate() {
        // Given
        Resource resource = new Resource("doc", "content", "text/plain", null);
        resource.setId(UUID.randomUUID());
        resource.setStatus(ResourceStatus.ERROR);

        // When
        adapter.updateStatus(resource);

        // Then
        verify(jpaRepository).updateStatus(resource.getId(), ResourceStatus.ERROR);
    }

    @Test
    void updateName_shouldUpdateNameOnExistingEntity() {
        // Given
        UUID id = UUID.randomUUID();
        ResourceEntity entity = new ResourceEntity();
        entity.setId(id);
        entity.setName("old-name");
        when(jpaRepository.findById(id)).thenReturn(Optional.of(entity));

        // When
        adapter.updateName(id, "new-name");

        // Then
        ArgumentCaptor<ResourceEntity> captor = ArgumentCaptor.forClass(ResourceEntity.class);
        verify(jpaRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("new-name");
    }

    @Test
    void updateName_shouldThrowNotFoundException_whenResourceDoesNotExist() {
        // Given
        UUID id = UUID.randomUUID();
        when(jpaRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adapter.updateName(id, "new-name"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void hasCurrentUserWriteAccess_shouldReturnTrue_whenCurrentUserIsCreator() {
        // Given
        UUID id = UUID.randomUUID();
        ResourceEntity entity = new ResourceEntity();
        entity.setId(id);
        entity.setCreatedBy("alice");
        when(jpaRepository.findById(id)).thenReturn(Optional.of(entity));
        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");

        // When & Then
        assertThat(adapter.hasCurrentUserWriteAccess(id)).isTrue();
    }

    @Test
    void hasCurrentUserWriteAccess_shouldDelegateToGroupCheck_whenNotCreator() {
        // Given
        UUID id = UUID.randomUUID();
        ResourceEntity entity = new ResourceEntity();
        entity.setId(id);
        entity.setCreatedBy("bob");
        when(jpaRepository.findById(id)).thenReturn(Optional.of(entity));
        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");
        when(jpaRepository.hasGroupWriteAccess(id, "alice")).thenReturn(true);

        // When & Then
        assertThat(adapter.hasCurrentUserWriteAccess(id)).isTrue();
    }

    @Test
    void hasCurrentUserWriteAccess_shouldThrowNotFoundException_whenResourceMissing() {
        // Given
        UUID id = UUID.randomUUID();
        when(jpaRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adapter.hasCurrentUserWriteAccess(id))
                .isInstanceOf(NotFoundException.class);
    }
}
