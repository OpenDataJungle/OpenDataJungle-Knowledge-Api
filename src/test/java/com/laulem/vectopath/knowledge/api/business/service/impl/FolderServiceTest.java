package com.laulem.vectopath.knowledge.api.business.service.impl;

import com.laulem.vectopath.knowledge.api.business.exception.NotFoundException;
import com.laulem.vectopath.knowledge.api.business.exception.ParamException;
import com.laulem.vectopath.knowledge.api.business.model.Folder;
import com.laulem.vectopath.knowledge.api.business.repository.FolderRepository;
import com.laulem.vectopath.knowledge.api.business.service.AuthenticationUseCase;
import com.laulem.vectopath.knowledge.api.business.service.ReferentialUseCase;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FolderServiceTest {
    @Mock
    private FolderRepository folderRepository;

    @Mock
    private AuthenticationUseCase authenticationUseCase;

    @Mock
    private ReferentialUseCase referentialUseCase;

    @InjectMocks
    private FolderService service;

    @Test
    void create_shouldResolveAndSetParentId_whenParentExistsAndIsWritable() {
        // Given
        Folder folder = new Folder("docs", "/root", List.of(), "alice");
        UUID parentId = UUID.randomUUID();
        when(folderRepository.findFolderIdByCompletePath("/root")).thenReturn(Optional.of(parentId));
        when(folderRepository.hasCurrentUserWriteAccess(parentId)).thenReturn(true);
        when(folderRepository.existsByCompletePath("/root/docs")).thenReturn(false);
        when(folderRepository.save(folder)).thenReturn(folder);

        // When
        service.create(folder);

        // Then
        assertThat(folder.getParentId()).isEqualTo(parentId);
    }

    @Test
    void create_shouldFetchDefaultGroupIds_whenNoneProvided() {
        // Given
        Folder folder = new Folder("docs", "/root", List.of(), "alice");
        UUID parentId = UUID.randomUUID();
        UUID defaultGroupId = UUID.randomUUID();
        when(folderRepository.findFolderIdByCompletePath("/root")).thenReturn(Optional.of(parentId));
        when(folderRepository.hasCurrentUserWriteAccess(parentId)).thenReturn(true);
        when(folderRepository.existsByCompletePath("/root/docs")).thenReturn(false);
        when(folderRepository.getFolderGroupsIdByCompletePath("/root")).thenReturn(List.of(defaultGroupId));
        when(folderRepository.save(folder)).thenReturn(folder);

        // When
        service.create(folder);

        // Then
        assertThat(folder.getGroupIds()).containsExactly(defaultGroupId);
    }

    @Test
    void create_shouldThrowParamException_whenNameIsNull() {
        // Given
        Folder folder = new Folder(null, "/root", List.of(), "alice");

        // When & Then
        assertThatThrownBy(() -> service.create(folder))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getCode()).isEqualTo("FOLDER_PATH_OR_NAME_NULL"));
    }

    @Test
    void create_shouldThrowParamException_whenPathIsNull() {
        // Given
        Folder folder = new Folder("docs", null, List.of(), "alice");

        // When & Then
        assertThatThrownBy(() -> service.create(folder))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getCode()).isEqualTo("FOLDER_PATH_OR_NAME_NULL"));
    }

    @Test
    void create_shouldThrowParamException_whenGroupPermissionsNotWritable() {
        // Given
        UUID groupId = UUID.randomUUID();
        Folder folder = new Folder("docs", "/root", List.of(groupId), "alice");
        when(referentialUseCase.hasCurrentUserWriteGroupAccess(List.of(groupId))).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> service.create(folder))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getCode()).isEqualTo("FOLDER_GROUP_ACCESS_DENIED"));
    }

    @Test
    void create_shouldSucceed_whenGroupPermissionsAreWritable() {
        // Given
        UUID groupId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        Folder folder = new Folder("docs", "/root", List.of(groupId), "alice");
        when(referentialUseCase.hasCurrentUserWriteGroupAccess(List.of(groupId))).thenReturn(true);
        when(folderRepository.findFolderIdByCompletePath("/root")).thenReturn(Optional.of(parentId));
        when(folderRepository.hasCurrentUserWriteAccess(parentId)).thenReturn(true);
        when(folderRepository.existsByCompletePath("/root/docs")).thenReturn(false);
        when(folderRepository.save(folder)).thenReturn(folder);

        // When
        Folder result = service.create(folder);

        // Then
        assertThat(result).isSameAs(folder);
        assertThat(folder.getGroupIds()).containsExactly(groupId);
    }

    @Test
    void create_shouldThrowParamException_whenParentPathDoesNotExist() {
        // Given
        Folder folder = new Folder("docs", "/unknown", List.of(), "alice");
        when(folderRepository.findFolderIdByCompletePath("/unknown")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.create(folder))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getCode()).isEqualTo("FOLDER_PARENT_NOT_FOUND"));
    }

    @Test
    void create_shouldThrowParamException_whenNoWriteAccessToParent() {
        // Given
        Folder folder = new Folder("docs", "/root", List.of(), "alice");
        UUID parentId = UUID.randomUUID();
        when(folderRepository.findFolderIdByCompletePath("/root")).thenReturn(Optional.of(parentId));
        when(folderRepository.hasCurrentUserWriteAccess(parentId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> service.create(folder))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getCode()).isEqualTo("FOLDER_PARENT_ACCESS_DENIED"));
    }

    @Test
    void create_shouldThrowParamException_whenPathAlreadyExists() {
        // Given
        Folder folder = new Folder("docs", "/root", List.of(), "alice");
        UUID parentId = UUID.randomUUID();
        when(folderRepository.findFolderIdByCompletePath("/root")).thenReturn(Optional.of(parentId));
        when(folderRepository.hasCurrentUserWriteAccess(parentId)).thenReturn(true);
        when(folderRepository.existsByCompletePath("/root/docs")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> service.create(folder))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getCode()).isEqualTo("FOLDER_PATH_EXISTS"));
    }

    @Test
    void getById_shouldReturnFolder_whenFound() {
        // Given
        UUID id = UUID.randomUUID();
        Folder folder = new Folder("docs", "/root", List.of(), "alice");
        folder.setId(id);
        when(folderRepository.findByIdWithAccessControl(id)).thenReturn(Optional.of(folder));

        // When
        Folder result = service.getById(id);

        // Then
        assertThat(result).isSameAs(folder);
    }

    @Test
    void getById_shouldThrowNotFoundException_whenMissing() {
        // Given
        UUID id = UUID.randomUUID();
        when(folderRepository.findByIdWithAccessControl(id)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listAll_shouldDelegateToRepository() {
        // Given
        Folder folder = new Folder("docs", "/root", List.of(), "alice");
        when(folderRepository.findAllWithAccessControl()).thenReturn(List.of(folder));

        // When
        List<Folder> result = service.listAll();

        // Then
        assertThat(result).containsExactly(folder);
    }

    @Test
    void findAllChildren_shouldReturnChildren_whenParentExists() {
        // Given
        UUID parentId = UUID.randomUUID();
        Folder parent = new Folder("docs", "/root", List.of(), "alice");
        Folder child = new Folder("sub", "/root/docs", List.of(), "alice");
        when(folderRepository.findByIdWithAccessControl(parentId)).thenReturn(Optional.of(parent));
        when(folderRepository.findAllChildrenWithAccessControl(parentId)).thenReturn(List.of(child));

        // When
        List<Folder> result = service.findAllChildren(parentId);

        // Then
        assertThat(result).containsExactly(child);
    }

    @Test
    void findAllChildren_shouldThrowNotFoundException_whenParentMissing() {
        // Given
        UUID parentId = UUID.randomUUID();
        when(folderRepository.findByIdWithAccessControl(parentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.findAllChildren(parentId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void update_shouldPersistNewGroupIdsAndRecalculatedParentId() {
        // Given
        UUID id = UUID.randomUUID();
        UUID newGroupId = UUID.randomUUID();
        UUID newParentId = UUID.randomUUID();
        Folder request = new Folder();
        request.setId(id);
        request.setName("renamed");
        request.setPath("/new-parent");
        request.setGroupIds(List.of(newGroupId));

        Folder existing = new Folder("old-name", "/old-parent", List.of(UUID.randomUUID()), "alice");
        existing.setId(id);

        when(folderRepository.hasCurrentUserWriteAccess(id)).thenReturn(true);
        when(referentialUseCase.hasCurrentUserWriteGroupAccess(List.of(newGroupId))).thenReturn(true);
        when(folderRepository.findById(id)).thenReturn(Optional.of(existing));
        when(folderRepository.findFolderIdByCompletePath("/new-parent")).thenReturn(Optional.of(newParentId));
        when(folderRepository.hasCurrentUserWriteAccess(newParentId)).thenReturn(true);
        when(folderRepository.existsByCompletePath("/new-parent/renamed")).thenReturn(false);
        when(folderRepository.save(existing)).thenReturn(existing);

        // When
        Folder result = service.update(request);

        // Then
        assertThat(result.getParentId()).isEqualTo(newParentId);
        assertThat(result.getGroupIds()).containsExactly(newGroupId);
        assertThat(result.getName()).isEqualTo("renamed");
        assertThat(result.getPath()).isEqualTo("/new-parent");
    }

    @Test
    void update_shouldSkipPathExistsCheck_whenCompletePathUnchanged() {
        // Given
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        Folder request = new Folder();
        request.setId(id);
        request.setName("docs");
        request.setPath("/root");

        Folder existing = new Folder("docs", "/root", List.of(), "alice");
        existing.setId(id);

        when(folderRepository.hasCurrentUserWriteAccess(id)).thenReturn(true);
        when(folderRepository.findById(id)).thenReturn(Optional.of(existing));
        when(folderRepository.findFolderIdByCompletePath("/root")).thenReturn(Optional.of(parentId));
        when(folderRepository.hasCurrentUserWriteAccess(parentId)).thenReturn(true);
        when(folderRepository.save(existing)).thenReturn(existing);

        // When
        Folder result = service.update(request);

        // Then
        assertThat(result.getName()).isEqualTo("docs");
        verify(folderRepository, never()).existsByCompletePath(anyString());
    }

    @Test
    void update_shouldThrowParamException_whenNameIsNull() {
        // Given
        UUID id = UUID.randomUUID();
        Folder request = new Folder();
        request.setId(id);
        request.setName(null);
        request.setPath("/root");

        // When & Then
        assertThatThrownBy(() -> service.update(request))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getCode()).isEqualTo("FOLDER_PATH_OR_NAME_NULL"));
    }

    @Test
    void update_shouldThrowParamException_whenPathIsNull() {
        // Given
        UUID id = UUID.randomUUID();
        Folder request = new Folder();
        request.setId(id);
        request.setName("renamed");
        request.setPath(null);

        // When & Then
        assertThatThrownBy(() -> service.update(request))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getCode()).isEqualTo("FOLDER_PATH_OR_NAME_NULL"));
    }

    @Test
    void update_shouldThrowParamException_whenAddingGroupWithoutWriteAccess() {
        // Given
        UUID id = UUID.randomUUID();
        UUID newGroupId = UUID.randomUUID();
        Folder request = new Folder();
        request.setId(id);
        request.setName("renamed");
        request.setPath("/root");
        request.setGroupIds(List.of(newGroupId));

        Folder existing = new Folder("old-name", "/root", List.of(), "alice");
        existing.setId(id);

        when(folderRepository.hasCurrentUserWriteAccess(id)).thenReturn(true);
        when(folderRepository.findById(id)).thenReturn(Optional.of(existing));
        when(referentialUseCase.hasCurrentUserWriteGroupAccess(List.of(newGroupId))).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> service.update(request))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getCode()).isEqualTo("FOLDER_GROUP_ACCESS_DENIED"));
    }

    @Test
    void update_shouldAllowRemovingGroup_withoutWriteAccessToIt() {
        // Given
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        UUID existingGroupId = UUID.randomUUID();
        Folder request = new Folder();
        request.setId(id);
        request.setName("renamed");
        request.setPath("/root");
        request.setGroupIds(List.of());

        Folder existing = new Folder("old-name", "/root", List.of(existingGroupId), "alice");
        existing.setId(id);

        when(folderRepository.hasCurrentUserWriteAccess(id)).thenReturn(true);
        when(folderRepository.findById(id)).thenReturn(Optional.of(existing));
        when(folderRepository.findFolderIdByCompletePath("/root")).thenReturn(Optional.of(parentId));
        when(folderRepository.hasCurrentUserWriteAccess(parentId)).thenReturn(true);
        when(folderRepository.getFolderGroupsIdByCompletePath("/root")).thenReturn(List.of());
        when(folderRepository.save(existing)).thenReturn(existing);

        // When
        Folder result = service.update(request);

        // Then
        assertThat(result.getGroupIds()).isEmpty();
    }

    @Test
    void update_shouldAllowKeepingGroup_withoutWriteAccessToIt() {
        // Given
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        UUID existingGroupId = UUID.randomUUID();
        Folder request = new Folder();
        request.setId(id);
        request.setName("renamed");
        request.setPath("/root");
        request.setGroupIds(List.of(existingGroupId));

        Folder existing = new Folder("old-name", "/root", List.of(existingGroupId), "alice");
        existing.setId(id);

        when(folderRepository.hasCurrentUserWriteAccess(id)).thenReturn(true);
        when(folderRepository.findById(id)).thenReturn(Optional.of(existing));
        when(folderRepository.findFolderIdByCompletePath("/root")).thenReturn(Optional.of(parentId));
        when(folderRepository.hasCurrentUserWriteAccess(parentId)).thenReturn(true);
        when(folderRepository.save(existing)).thenReturn(existing);

        // When
        Folder result = service.update(request);

        // Then
        assertThat(result.getGroupIds()).containsExactly(existingGroupId);
    }

    @Test
    void update_shouldThrowParamException_whenNewParentPathDoesNotExist() {
        // Given
        UUID id = UUID.randomUUID();
        Folder request = new Folder();
        request.setId(id);
        request.setName("renamed");
        request.setPath("/unknown");

        Folder existing = new Folder("old-name", "/old-parent", List.of(), "alice");
        existing.setId(id);

        when(folderRepository.hasCurrentUserWriteAccess(id)).thenReturn(true);
        when(folderRepository.findById(id)).thenReturn(Optional.of(existing));
        when(folderRepository.findFolderIdByCompletePath("/unknown")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.update(request))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getCode()).isEqualTo("FOLDER_PARENT_NOT_FOUND"));
    }

    @Test
    void update_shouldThrowParamException_whenNoWriteAccessToNewParent() {
        // Given
        UUID id = UUID.randomUUID();
        UUID newParentId = UUID.randomUUID();
        Folder request = new Folder();
        request.setId(id);
        request.setName("renamed");
        request.setPath("/other-parent");

        Folder existing = new Folder("old-name", "/old-parent", List.of(), "alice");
        existing.setId(id);

        when(folderRepository.hasCurrentUserWriteAccess(id)).thenReturn(true);
        when(folderRepository.findById(id)).thenReturn(Optional.of(existing));
        when(folderRepository.findFolderIdByCompletePath("/other-parent")).thenReturn(Optional.of(newParentId));
        when(folderRepository.hasCurrentUserWriteAccess(newParentId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> service.update(request))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getCode()).isEqualTo("FOLDER_PARENT_ACCESS_DENIED"));
    }

    @Test
    void update_shouldThrowParamException_whenNoWriteAccessToFolderItself() {
        // Given
        UUID id = UUID.randomUUID();
        Folder request = new Folder();
        request.setId(id);
        request.setName("renamed");
        request.setPath("/root");

        when(folderRepository.hasCurrentUserWriteAccess(id)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> service.update(request))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getCode()).isEqualTo("FOLDER_ACCESS_DENIED"));
    }

    @Test
    void update_shouldThrowParamException_whenTargetPathAlreadyUsedByAnotherFolder() {
        // Given
        UUID id = UUID.randomUUID();
        Folder request = new Folder();
        request.setId(id);
        request.setName("renamed");
        request.setPath("/new-parent");

        Folder existing = new Folder("old-name", "/old-parent", List.of(), "alice");
        existing.setId(id);

        when(folderRepository.hasCurrentUserWriteAccess(id)).thenReturn(true);
        when(folderRepository.findById(id)).thenReturn(Optional.of(existing));
        when(folderRepository.existsByCompletePath("/new-parent/renamed")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> service.update(request))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getCode()).isEqualTo("FOLDER_PATH_EXISTS"));
    }

    @Test
    void update_shouldThrowNotFoundException_whenFolderDoesNotExist() {
        // Given
        UUID id = UUID.randomUUID();
        Folder request = new Folder();
        request.setId(id);
        request.setName("renamed");
        request.setPath("/root");

        when(folderRepository.hasCurrentUserWriteAccess(id)).thenReturn(true);
        when(folderRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.update(request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void delete_shouldDeleteFolder_whenCurrentUserHasWriteAccess() {
        // Given
        UUID id = UUID.randomUUID();
        when(folderRepository.hasCurrentUserWriteAccess(id)).thenReturn(true);

        // When
        service.delete(id);

        // Then
        verify(folderRepository).deleteById(id);
    }

    @Test
    void delete_shouldThrowNotFoundException_whenNoWriteAccess() {
        // Given
        UUID id = UUID.randomUUID();
        when(folderRepository.hasCurrentUserWriteAccess(id)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(NotFoundException.class);
        verify(folderRepository, never()).deleteById(any());
    }

    @Test
    void getOrCreateDefaultFolder_shouldReturnExistingFolder_whenAlreadyExists() {
        // Given
        String username = "alice";
        Folder existing = new Folder(username, "ROOT/USERS", List.of(), username);
        when(authenticationUseCase.getCurrentUser()).thenReturn(username);
        when(folderRepository.findByCompletePath("ROOT/USERS/alice")).thenReturn(Optional.of(existing));

        // When
        Folder result = service.getOrCreateDefaultFolder();

        // Then
        assertThat(result).isSameAs(existing);
        verify(folderRepository, never()).save(any());
    }

    @Test
    void getOrCreateDefaultFolder_shouldCreateFolder_whenNotExists() {
        // Given
        String username = "alice";
        when(authenticationUseCase.getCurrentUser()).thenReturn(username);
        when(folderRepository.findByCompletePath("ROOT/USERS/alice")).thenReturn(Optional.empty());
        ArgumentCaptor<Folder> folderCaptor = ArgumentCaptor.forClass(Folder.class);
        Folder saved = new Folder(username, "ROOT/USERS", List.of(), username);
        when(folderRepository.save(folderCaptor.capture())).thenReturn(saved);

        // When
        Folder result = service.getOrCreateDefaultFolder();

        // Then
        assertThat(result).isSameAs(saved);
        Folder captured = folderCaptor.getValue();
        assertThat(captured.getName()).isEqualTo(username);
        assertThat(captured.getPath()).isEqualTo("ROOT/USERS");
        assertThat(captured.getCreatedBy()).isEqualTo(username);
    }

    @Test
    void hasCurrentUserWriteAccess_shouldDelegateToRepository() {
        // Given
        UUID id = UUID.randomUUID();
        when(folderRepository.hasCurrentUserWriteAccess(id)).thenReturn(true);

        // When
        boolean result = service.hasCurrentUserWriteAccess(id);

        // Then
        assertThat(result).isTrue();
    }
}
