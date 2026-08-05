package com.laulem.vectopath.knowledge.api.infra.repository;

import com.laulem.vectopath.knowledge.api.business.exception.NotFoundException;
import com.laulem.vectopath.knowledge.api.business.model.Folder;
import com.laulem.vectopath.knowledge.api.business.service.AuthenticationUseCase;
import com.laulem.vectopath.knowledge.api.infra.entity.FolderEntity;
import com.laulem.vectopath.knowledge.api.infra.entity.GroupEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FolderRepositoryAdapterTest {

    @Mock
    private FolderJpaRepository folderJpaRepository;

    @Mock
    private AuthenticationUseCase authenticationUseCase;

    @Mock
    private ReferentialRepository referentialRepository;

    @InjectMocks
    private FolderRepositoryAdapter adapter;

    @Test
    void save_shouldMapGroupIdsToGroupEntities() {
        // Given
        UUID groupId1 = UUID.randomUUID();
        UUID groupId2 = UUID.randomUUID();
        Folder folder = new Folder("docs", "/root", List.of(groupId1, groupId2), "alice");
        FolderEntity savedEntity = new FolderEntity();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setName("docs");
        when(folderJpaRepository.save(any(FolderEntity.class))).thenReturn(savedEntity);

        ArgumentCaptor<FolderEntity> captor = ArgumentCaptor.forClass(FolderEntity.class);

        // When
        Folder result = adapter.save(folder);

        // Then
        assertThat(result.getId()).isEqualTo(savedEntity.getId());
        verify(folderJpaRepository).save(captor.capture());
        Set<UUID> capturedGroupIds = captor.getValue().getGroups().stream().map(GroupEntity::getId).collect(Collectors.toSet());
        assertThat(capturedGroupIds).containsExactlyInAnyOrder(groupId1, groupId2);
    }

    @Test
    void save_shouldMapToEmptyGroupSet_whenGroupIdsIsNull() {
        // Given
        Folder folder = new Folder("docs", "/root", null, "alice");
        when(folderJpaRepository.save(any(FolderEntity.class))).thenReturn(new FolderEntity());

        ArgumentCaptor<FolderEntity> captor = ArgumentCaptor.forClass(FolderEntity.class);

        // When
        adapter.save(folder);

        // Then
        verify(folderJpaRepository).save(captor.capture());
        assertThat(captor.getValue().getGroups()).isEmpty();
    }

    @Test
    void findById_shouldReturnMappedFolder_whenPresent() {
        // Given
        UUID id = UUID.randomUUID();
        FolderEntity entity = new FolderEntity();
        entity.setId(id);
        entity.setName("docs");
        when(folderJpaRepository.findById(id)).thenReturn(Optional.of(entity));

        // When
        Optional<Folder> result = adapter.findById(id);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(id);
    }

    @Test
    void findById_shouldReturnEmpty_whenAbsent() {
        // Given
        UUID id = UUID.randomUUID();
        when(folderJpaRepository.findById(id)).thenReturn(Optional.empty());

        // When
        Optional<Folder> result = adapter.findById(id);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findByCompletePath_shouldReturnMappedFolder() {
        // Given
        FolderEntity entity = new FolderEntity();
        entity.setId(UUID.randomUUID());
        when(folderJpaRepository.findByCompletePath("/root/docs")).thenReturn(Optional.of(entity));

        // When
        Optional<Folder> result = adapter.findByCompletePath("/root/docs");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(entity.getId());
    }

    @Test
    void findByIdWithAccessControl_shouldQueryUsingCurrentUser() {
        // Given
        UUID id = UUID.randomUUID();
        FolderEntity entity = new FolderEntity();
        entity.setId(id);
        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");
        when(folderJpaRepository.findByIdWithAccessControl(id, "alice")).thenReturn(Optional.of(entity));

        // When
        Optional<Folder> result = adapter.findByIdWithAccessControl(id);

        // Then
        assertThat(result).isPresent();
    }

    @Test
    void existsById_shouldDelegate() {
        // Given
        UUID id = UUID.randomUUID();
        when(folderJpaRepository.existsById(id)).thenReturn(true);

        // When & Then
        assertThat(adapter.existsById(id)).isTrue();
    }

    @Test
    void existsByCompletePath_shouldDelegate() {
        // Given
        when(folderJpaRepository.existsByCompletePath("/root/docs")).thenReturn(true);

        // When & Then
        assertThat(adapter.existsByCompletePath("/root/docs")).isTrue();
    }

    @Test
    void findAllWithAccessControl_shouldMapEntity() {
        // Given
        FolderEntity entity = new FolderEntity();
        entity.setId(UUID.randomUUID());
        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");
        when(folderJpaRepository.findAllWithAccessControl("alice")).thenReturn(List.of(entity));

        // When
        List<Folder> result = adapter.findAllWithAccessControl();

        // Then
        assertThat(result).extracting(Folder::getId).containsExactly(entity.getId());
    }

    @Test
    void findAllChildrenWithAccessControl_shouldMapEntity() {
        // Given
        UUID parentId = UUID.randomUUID();
        FolderEntity child = new FolderEntity();
        child.setId(UUID.randomUUID());
        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");
        when(folderJpaRepository.findAllChildrenWithAccessControl(parentId, "alice")).thenReturn(List.of(child));

        // When
        List<Folder> result = adapter.findAllChildrenWithAccessControl(parentId);

        // Then
        assertThat(result).extracting(Folder::getId).containsExactly(child.getId());
    }

    @Test
    void deleteById_shouldDelegate() {
        // Given
        UUID id = UUID.randomUUID();

        // When
        adapter.deleteById(id);

        // Then
        verify(folderJpaRepository).deleteById(id);
    }

    @Test
    void hasCurrentUserWriteAccess_shouldReturnTrue_whenCurrentUserIsCreator() {
        // Given
        UUID id = UUID.randomUUID();
        FolderEntity entity = new FolderEntity();
        entity.setId(id);
        entity.setCreatedBy("alice");
        when(folderJpaRepository.findById(id)).thenReturn(Optional.of(entity));
        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");

        // When & Then
        assertThat(adapter.hasCurrentUserWriteAccess(id)).isTrue();
    }

    @Test
    void hasCurrentUserWriteAccess_shouldReturnTrue_whenUserGroupHasWriteAccess() {
        // Given
        UUID id = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        GroupEntity group = new GroupEntity();
        group.setId(groupId);
        FolderEntity entity = new FolderEntity();
        entity.setId(id);
        entity.setCreatedBy("bob");
        entity.setGroups(Set.of(group));
        when(folderJpaRepository.findById(id)).thenReturn(Optional.of(entity));
        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");
        when(referentialRepository.getGroupWriteAccess("alice")).thenReturn(List.of(groupId));

        // When & Then
        assertThat(adapter.hasCurrentUserWriteAccess(id)).isTrue();
    }

    @Test
    void hasCurrentUserWriteAccess_shouldReturnFalse_whenNeitherOwnerNorGroupGrantsAccess() {
        // Given
        UUID id = UUID.randomUUID();
        FolderEntity entity = new FolderEntity();
        entity.setId(id);
        entity.setCreatedBy("bob");
        entity.setGroups(Set.of());
        when(folderJpaRepository.findById(id)).thenReturn(Optional.of(entity));
        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");
        when(referentialRepository.getGroupWriteAccess("alice")).thenReturn(List.of());

        // When & Then
        assertThat(adapter.hasCurrentUserWriteAccess(id)).isFalse();
    }

    @Test
    void hasCurrentUserWriteAccess_shouldThrowNotFoundException_whenFolderMissing() {
        // Given
        UUID id = UUID.randomUUID();
        when(folderJpaRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adapter.hasCurrentUserWriteAccess(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findFolderIdByCompletePath_shouldReturnId_whenFound() {
        // Given
        UUID id = UUID.randomUUID();
        FolderEntity entity = new FolderEntity();
        entity.setId(id);
        when(folderJpaRepository.findByCompletePath("/root")).thenReturn(Optional.of(entity));

        // When
        Optional<UUID> result = adapter.findFolderIdByCompletePath("/root");

        // Then
        assertThat(result).contains(id);
    }

    @Test
    void findFolderIdByCompletePath_shouldReturnEmpty_whenNotFound() {
        // Given
        when(folderJpaRepository.findByCompletePath("/unknown")).thenReturn(Optional.empty());

        // When
        Optional<UUID> result = adapter.findFolderIdByCompletePath("/unknown");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getFolderGroupsIdByCompletePath_shouldReturnGroupIds_whenFolderExists() {
        // Given
        UUID groupId = UUID.randomUUID();
        GroupEntity group = new GroupEntity();
        group.setId(groupId);
        FolderEntity entity = new FolderEntity();
        entity.setGroups(Set.of(group));
        when(folderJpaRepository.findByCompletePath("/root")).thenReturn(Optional.of(entity));

        // When
        List<UUID> result = adapter.getFolderGroupsIdByCompletePath("/root");

        // Then
        assertThat(result).containsExactly(groupId);
    }

    @Test
    void getFolderGroupsIdByCompletePath_shouldReturnEmptyList_whenFolderDoesNotExist() {
        // Given
        when(folderJpaRepository.findByCompletePath("/unknown")).thenReturn(Optional.empty());

        // When
        List<UUID> result = adapter.getFolderGroupsIdByCompletePath("/unknown");

        // Then
        assertThat(result).isEmpty();
    }
}
