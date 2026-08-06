package com.laulem.vectopath.knowledge.api.business.service.impl;

import com.laulem.vectopath.knowledge.api.business.exception.NotFoundException;
import com.laulem.vectopath.knowledge.api.business.exception.ParamException;
import com.laulem.vectopath.knowledge.api.business.exception.VectorizationException;
import com.laulem.vectopath.knowledge.api.business.model.Folder;
import com.laulem.vectopath.knowledge.api.business.model.Resource;
import com.laulem.vectopath.knowledge.api.business.model.ResourceGroupPermission;
import com.laulem.vectopath.knowledge.api.business.model.ResourceStatus;
import com.laulem.vectopath.knowledge.api.business.repository.ResourceRepository;
import com.laulem.vectopath.knowledge.api.business.repository.VectorStoreRepository;
import com.laulem.vectopath.knowledge.api.business.service.AuthenticationUseCase;
import com.laulem.vectopath.knowledge.api.business.service.FolderUseCase;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private VectorStoreRepository vectorRepository;

    @Mock
    private FolderUseCase folderUseCase;

    @Mock
    private AuthenticationUseCase authenticationUseCase;

    @Mock
    private ReferentialUseCase referentialUseCase;

    @InjectMocks
    private ResourceService service;

    @Test
    void createResource_shouldAssignDefaultFolder_whenFolderIdIsNull() {
        // Given
        Resource resource = new Resource("doc", "content", "text/plain", null);
        UUID defaultFolderId = UUID.randomUUID();
        Folder defaultFolder = new Folder();
        defaultFolder.setId(defaultFolderId);
        when(folderUseCase.getOrCreateDefaultFolder()).thenReturn(defaultFolder);
        when(resourceRepository.save(any(Resource.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Resource result = service.createResource(resource);

        // Then
        assertThat(result.getFolderId()).isEqualTo(defaultFolderId);
        assertThat(result.getStatus()).isEqualTo(ResourceStatus.VECTORIZED);
    }

    @Test
    void createResource_shouldThrowNotFoundException_whenNoWriteAccessToSpecifiedFolder() {
        // Given
        UUID folderId = UUID.randomUUID();
        Resource resource = new Resource("doc", "content", "text/plain", null);
        resource.setFolderId(folderId);
        when(folderUseCase.hasCurrentUserWriteAccess(folderId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> service.createResource(resource))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createResource_shouldThrowParamException_whenGroupPermissionsNotWritable() {
        // Given
        UUID folderId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        Resource resource = new Resource("doc", "content", "text/plain", null);
        resource.setFolderId(folderId);
        resource.setGroupPermissions(List.of(new ResourceGroupPermission(groupId, UUID.randomUUID())));
        when(folderUseCase.hasCurrentUserWriteAccess(folderId)).thenReturn(true);
        when(referentialUseCase.hasCurrentUserWriteGroupAccess(List.of(groupId))).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> service.createResource(resource))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getCode()).isEqualTo("RESOURCE_GROUP_ACCESS_DENIED"));
    }

    @Test
    void createResource_shouldSucceed_whenGroupPermissionsAreWritable() {
        // Given
        UUID folderId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        Resource resource = new Resource("doc", "content", "text/plain", null);
        resource.setFolderId(folderId);
        resource.setGroupPermissions(List.of(new ResourceGroupPermission(groupId, UUID.randomUUID())));
        when(folderUseCase.hasCurrentUserWriteAccess(folderId)).thenReturn(true);
        when(referentialUseCase.hasCurrentUserWriteGroupAccess(List.of(groupId))).thenReturn(true);
        when(resourceRepository.save(any(Resource.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Resource result = service.createResource(resource);

        // Then
        assertThat(result.getStatus()).isEqualTo(ResourceStatus.VECTORIZED);
    }

    @Test
    void createResource_shouldMarkErrorAndThrowVectorizationException_whenVectorizationFails() {
        // Given
        Resource resource = new Resource("doc", "content", "text/plain", null);
        Folder defaultFolder = new Folder();
        defaultFolder.setId(UUID.randomUUID());
        when(folderUseCase.getOrCreateDefaultFolder()).thenReturn(defaultFolder);
        when(resourceRepository.save(any(Resource.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("embedding failed")).when(vectorRepository).addResource(any(Resource.class));
        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);

        // When & Then
        assertThatThrownBy(() -> service.createResource(resource))
                .isInstanceOf(VectorizationException.class);

        verify(resourceRepository).updateStatus(resourceCaptor.capture());
        assertThat(resourceCaptor.getValue().getStatus()).isEqualTo(ResourceStatus.ERROR);
    }

    @Test
    void findById_shouldDelegateToRepository() {
        // Given
        UUID id = UUID.randomUUID();
        Resource resource = new Resource();
        when(resourceRepository.findByIdWithAccessControl(id)).thenReturn(Optional.of(resource));

        // When
        Optional<Resource> result = service.findById(id);

        // Then
        assertThat(result).contains(resource);
    }

    @Test
    void findAll_shouldDelegateToRepository() {
        // Given
        Resource resource = new Resource();
        when(resourceRepository.findAllWithAccessControl()).thenReturn(List.of(resource));

        // When
        List<Resource> result = service.findAll();

        // Then
        assertThat(result).containsExactly(resource);
    }

    @Test
    void findByStatus_shouldDelegateToRepository() {
        // Given
        Resource resource = new Resource();
        when(resourceRepository.findByStatusWithAccessControl(ResourceStatus.VECTORIZED)).thenReturn(List.of(resource));

        // When
        List<Resource> result = service.findByStatus(ResourceStatus.VECTORIZED);

        // Then
        assertThat(result).containsExactly(resource);
    }

    @Test
    void searchResources_shouldThrowParamException_whenNameAndPathAreBothBlank() {
        // When & Then
        assertThatThrownBy(() -> service.searchResources(" ", null))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getCode()).isEqualTo("REQUIRED"));
    }

    @Test
    void searchResources_shouldDelegateToRepository_whenNameIsProvided() {
        // Given
        Resource resource = new Resource();
        when(resourceRepository.searchWithAccessControl("report", null)).thenReturn(List.of(resource));

        // When
        List<Resource> result = service.searchResources("report", null);

        // Then
        assertThat(result).containsExactly(resource);
    }

    @Test
    void searchResources_shouldDelegateToRepository_whenPathIsProvided() {
        // Given
        Resource resource = new Resource();
        when(resourceRepository.searchWithAccessControl(null, "/docs")).thenReturn(List.of(resource));

        // When
        List<Resource> result = service.searchResources(null, "/docs");

        // Then
        assertThat(result).containsExactly(resource);
    }

    @Test
    void deleteResource_shouldDeleteVectorsAndResource_whenCurrentUserIsOwner() {
        // Given
        UUID id = UUID.randomUUID();
        Resource resource = new Resource("doc", "content", "text/plain", null);
        resource.setId(id);
        resource.setCreatedBy("alice");
        when(resourceRepository.findByIdWithAccessControl(id)).thenReturn(Optional.of(resource));
        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");

        // When
        service.deleteResource(id);

        // Then
        verify(vectorRepository).deleteResource(id);
        verify(resourceRepository).deleteById(id);
    }

    @Test
    void deleteResource_shouldThrowNotFoundException_whenResourceDoesNotExist() {
        // Given
        UUID id = UUID.randomUUID();
        when(resourceRepository.findByIdWithAccessControl(id)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.deleteResource(id))
                .isInstanceOf(NotFoundException.class);
        verify(vectorRepository, never()).deleteResource(any());
    }

    @Test
    void deleteResource_shouldThrowNotFoundException_whenCurrentUserHasNoWriteAccess() {
        // Given
        UUID id = UUID.randomUUID();
        UUID folderId = UUID.randomUUID();
        Resource resource = new Resource("doc", "content", "text/plain", null);
        resource.setId(id);
        resource.setCreatedBy("bob");
        resource.setFolderId(folderId);
        when(resourceRepository.findByIdWithAccessControl(id)).thenReturn(Optional.of(resource));
        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");
        when(folderUseCase.hasCurrentUserWriteAccess(folderId)).thenReturn(false);
        when(resourceRepository.hasCurrentUserWriteAccess(id)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> service.deleteResource(id))
                .isInstanceOf(NotFoundException.class);
        verify(vectorRepository, never()).deleteResource(any());
    }

    @Test
    void deleteResource_shouldAllowDeletion_whenResourceLevelGroupGrantsWriteAccess() {
        // Given
        UUID id = UUID.randomUUID();
        UUID folderId = UUID.randomUUID();
        Resource resource = new Resource("doc", "content", "text/plain", null);
        resource.setId(id);
        resource.setCreatedBy("bob");
        resource.setFolderId(folderId);
        when(resourceRepository.findByIdWithAccessControl(id)).thenReturn(Optional.of(resource));
        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");
        when(folderUseCase.hasCurrentUserWriteAccess(folderId)).thenReturn(false);
        when(resourceRepository.hasCurrentUserWriteAccess(id)).thenReturn(true);

        // When
        service.deleteResource(id);

        // Then
        verify(resourceRepository).deleteById(id);
    }

    @Test
    void deleteResource_shouldAllowDeletion_whenFolderLevelWriteAccessGranted() {
        // Given
        UUID id = UUID.randomUUID();
        UUID folderId = UUID.randomUUID();
        Resource resource = new Resource("doc", "content", "text/plain", null);
        resource.setId(id);
        resource.setCreatedBy("bob");
        resource.setFolderId(folderId);
        when(resourceRepository.findByIdWithAccessControl(id)).thenReturn(Optional.of(resource));
        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");
        when(folderUseCase.hasCurrentUserWriteAccess(folderId)).thenReturn(true);

        // When
        service.deleteResource(id);

        // Then
        verify(resourceRepository).deleteById(id);
        verify(resourceRepository, never()).hasCurrentUserWriteAccess(any());
    }

    @Test
    void deleteResource_shouldThrowNotFoundException_whenFolderIdIsNullAndCurrentUserIsNotOwner() {
        // Given
        UUID id = UUID.randomUUID();
        Resource resource = new Resource("doc", "content", "text/plain", null);
        resource.setId(id);
        resource.setCreatedBy("bob");
        resource.setFolderId(null);
        when(resourceRepository.findByIdWithAccessControl(id)).thenReturn(Optional.of(resource));
        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");

        // When & Then
        assertThatThrownBy(() -> service.deleteResource(id))
                .isInstanceOf(NotFoundException.class);
        verify(vectorRepository, never()).deleteResource(any());
        verify(folderUseCase, never()).hasCurrentUserWriteAccess(any());
    }

    @Test
    void reprocessResource_shouldDeleteOldVectorsAndReVectorize() {
        // Given
        UUID id = UUID.randomUUID();
        Resource resource = new Resource("doc", "content", "text/plain", null);
        resource.setId(id);
        resource.setCreatedBy("alice");
        when(resourceRepository.findByIdWithAccessControl(id)).thenReturn(Optional.of(resource));
        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");
        when(resourceRepository.save(any(Resource.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Resource result = service.reprocessResource(id);

        // Then
        assertThat(result.getStatus()).isEqualTo(ResourceStatus.VECTORIZED);
        verify(vectorRepository).deleteResource(id);
        verify(vectorRepository).addResource(resource);
    }

    @Test
    void renameResource_shouldUpdateName_whenCurrentUserIsOwner() {
        // Given
        UUID id = UUID.randomUUID();
        Resource resource = new Resource("doc", "content", "text/plain", null);
        resource.setId(id);
        resource.setCreatedBy("alice");
        when(resourceRepository.findByIdWithAccessControl(id)).thenReturn(Optional.of(resource));
        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");

        // When
        service.renameResource(id, "new-name");

        // Then
        verify(resourceRepository).updateName(id, "new-name");
    }
}
