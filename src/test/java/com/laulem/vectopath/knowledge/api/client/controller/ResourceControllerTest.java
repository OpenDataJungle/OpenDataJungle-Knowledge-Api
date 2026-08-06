package com.laulem.vectopath.knowledge.api.client.controller;

import com.laulem.vectopath.knowledge.api.business.exception.NotFoundException;
import com.laulem.vectopath.knowledge.api.business.model.Resource;
import com.laulem.vectopath.knowledge.api.business.model.ResourceGroupPermission;
import com.laulem.vectopath.knowledge.api.business.model.ResourceStatus;
import com.laulem.vectopath.knowledge.api.business.service.ResourceUseCase;
import com.laulem.vectopath.knowledge.api.client.dto.CreateResourceRequest;
import com.laulem.vectopath.knowledge.api.client.dto.RenameResourceRequest;
import com.laulem.vectopath.knowledge.api.client.dto.ResourceContentResponse;
import com.laulem.vectopath.knowledge.api.client.dto.ResourceGroupPermissionRequest;
import com.laulem.vectopath.knowledge.api.client.dto.ResourceGroupPermissionResponse;
import com.laulem.vectopath.knowledge.api.client.dto.ResourceResponse;
import com.laulem.vectopath.knowledge.api.client.service.ResourceCreationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceControllerTest {

    @Mock
    private ResourceUseCase resourceUseCase;

    @Mock
    private ResourceCreationService resourceCreationService;

    @InjectMocks
    private ResourceController controller;

    @Test
    void createResource_shouldReturnCreatedResourceMappedToAllResponseFields() throws Exception {
        // Given
        CreateResourceRequest request = new CreateResourceRequest("name", "content", null, "TEXT", null, null, null);
        Resource created = aResource();
        when(resourceCreationService.createGeneralResource(request)).thenReturn(created);

        // When
        ResourceResponse response = controller.createResource(request);

        // Then
        assertThat(response.id()).isEqualTo(created.getId());
        assertThat(response.name()).isEqualTo(created.getName());
        assertThat(response.contentType()).isEqualTo(created.getContentType());
        assertThat(response.status()).isEqualTo(created.getStatus());
        assertThat(response.metadata()).isEqualTo(created.getMetadata());
        assertThat(response.sourceType()).isEqualTo(created.getSourceType());
        assertThat(response.sourceName()).isEqualTo(created.getSourceName());
        assertThat(response.size()).isEqualTo(created.getSize());
        assertThat(response.createdBy()).isEqualTo(created.getCreatedBy());
        assertThat(response.folderId()).isEqualTo(created.getFolderId());
        assertThat(response.groupPermissions()).containsExactly(new ResourceGroupPermissionResponse(created.getGroupPermissions().getFirst()));
        assertThat(response.createdAt()).isEqualTo(created.getCreatedAt());
        assertThat(response.updatedAt()).isEqualTo(created.getUpdatedAt());
    }

    @Test
    void createResourceFromFile_shouldBuildFileRequest_andDelegateToCreationService() throws Exception {
        // Given
        MultipartFile file = new MockMultipartFile("file", "doc.txt", "text/plain", "hello".getBytes());
        UUID folderId = UUID.randomUUID();
        Resource created = aResource();
        when(resourceCreationService.createFileResource(any(CreateResourceRequest.class), eq(file))).thenReturn(created);
        ArgumentCaptor<CreateResourceRequest> requestCaptor = ArgumentCaptor.forClass(CreateResourceRequest.class);

        // When
        ResourceGroupPermissionRequest groupPermission = new ResourceGroupPermissionRequest(UUID.randomUUID(), UUID.randomUUID());
        List<ResourceGroupPermissionRequest> groupPermissions = List.of(groupPermission);

        ResourceResponse response = controller.createResourceFromFile(file, "doc", "metadata", folderId, groupPermissions);

        // Then
        assertThat(response.id()).isEqualTo(created.getId());
        verify(resourceCreationService).createFileResource(requestCaptor.capture(), eq(file));
        CreateResourceRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.name()).isEqualTo("doc");
        assertThat(capturedRequest.sourceType()).isEqualTo("file");
        assertThat(capturedRequest.folderId()).isEqualTo(folderId);
        assertThat(capturedRequest.metadata()).isEqualTo("metadata");
        assertThat(capturedRequest.groupPermissions()).isEqualTo(groupPermissions);
    }

    @Test
    void findAll_shouldMapEveryResourceToResponse() {
        // Given
        Resource first = aResource();
        Resource second = aResource();
        when(resourceUseCase.findAll()).thenReturn(List.of(first, second));

        // When
        List<ResourceResponse> responses = controller.findAll();

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).id()).isEqualTo(first.getId());
        assertThat(responses.get(0).name()).isEqualTo(first.getName());
        assertThat(responses.get(1).id()).isEqualTo(second.getId());
        assertThat(responses.get(1).name()).isEqualTo(second.getName());
    }

    @Test
    void getResourceById_shouldReturnResourceMappedToAllResponseFields() {
        // Given
        Resource resource = aResource();
        when(resourceUseCase.findById(resource.getId())).thenReturn(Optional.of(resource));

        // When
        ResourceResponse response = controller.getResourceById(resource.getId());

        // Then
        assertThat(response.id()).isEqualTo(resource.getId());
        assertThat(response.name()).isEqualTo(resource.getName());
        assertThat(response.contentType()).isEqualTo(resource.getContentType());
        assertThat(response.status()).isEqualTo(resource.getStatus());
        assertThat(response.metadata()).isEqualTo(resource.getMetadata());
        assertThat(response.sourceType()).isEqualTo(resource.getSourceType());
        assertThat(response.sourceName()).isEqualTo(resource.getSourceName());
        assertThat(response.size()).isEqualTo(resource.getSize());
        assertThat(response.createdBy()).isEqualTo(resource.getCreatedBy());
        assertThat(response.folderId()).isEqualTo(resource.getFolderId());
        assertThat(response.groupPermissions()).containsExactly(new ResourceGroupPermissionResponse(resource.getGroupPermissions().getFirst()));
        assertThat(response.createdAt()).isEqualTo(resource.getCreatedAt());
        assertThat(response.updatedAt()).isEqualTo(resource.getUpdatedAt());
    }

    @Test
    void getResourceById_shouldThrowNotFoundException_whenMissing() {
        // Given
        UUID id = UUID.randomUUID();
        when(resourceUseCase.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> controller.getResourceById(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void searchResources_shouldDelegateNameAndPath() {
        // Given
        Resource resource = aResource();
        when(resourceUseCase.searchResources("report", "/docs")).thenReturn(List.of(resource));

        // When
        List<ResourceResponse> responses = controller.searchResources("report", "/docs");

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(resource.getId());
        assertThat(responses.getFirst().name()).isEqualTo(resource.getName());
        verify(resourceUseCase).searchResources("report", "/docs");
    }

    @Test
    void findByStatus_shouldMapMatchingResources() {
        // Given
        Resource resource = aResource();
        when(resourceUseCase.findByStatus(ResourceStatus.VECTORIZED)).thenReturn(List.of(resource));

        // When
        List<ResourceResponse> responses = controller.findByStatus(ResourceStatus.VECTORIZED);

        // Then
        assertThat(responses).extracting(ResourceResponse::status).containsExactly(ResourceStatus.VECTORIZED);
        assertThat(responses).extracting(ResourceResponse::id).containsExactly(resource.getId());
    }

    @Test
    void getResourceContent_shouldReturnContent_whenFound() {
        // Given
        Resource resource = aResource();
        when(resourceUseCase.findById(resource.getId())).thenReturn(Optional.of(resource));

        // When
        ResourceContentResponse response = controller.getResourceContent(resource.getId());

        // Then
        assertThat(response.id()).isEqualTo(resource.getId());
        assertThat(response.name()).isEqualTo(resource.getName());
        assertThat(response.content()).isEqualTo(resource.getContent());
    }

    @Test
    void getResourceContent_shouldThrowNotFoundException_whenMissing() {
        // Given
        UUID id = UUID.randomUUID();
        when(resourceUseCase.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> controller.getResourceContent(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void reprocessResource_shouldReturnReprocessedResource() {
        // Given
        Resource resource = aResource();
        when(resourceUseCase.reprocessResource(resource.getId())).thenReturn(resource);

        // When
        ResourceResponse response = controller.reprocessResource(resource.getId());

        // Then
        assertThat(response.id()).isEqualTo(resource.getId());
        assertThat(response.name()).isEqualTo(resource.getName());
        assertThat(response.status()).isEqualTo(resource.getStatus());
    }

    @Test
    void renameResource_shouldDelegateToUseCase() {
        // Given
        UUID id = UUID.randomUUID();
        RenameResourceRequest request = new RenameResourceRequest("new-name");

        // When
        controller.renameResource(id, request);

        // Then
        verify(resourceUseCase).renameResource(id, "new-name");
    }

    @Test
    void deleteResource_shouldDelegateToUseCase() {
        // Given
        UUID id = UUID.randomUUID();

        // When
        controller.deleteResource(id);

        // Then
        verify(resourceUseCase).deleteResource(id);
    }

    private static Resource aResource() {
        Resource resource = new Resource("test", "content", "text/plain", "meta");
        resource.setId(UUID.randomUUID());
        resource.setStatus(ResourceStatus.VECTORIZED);
        resource.setSourceType("file");
        resource.setSourceName("test.txt");
        resource.setSize(42L);
        resource.setCreatedBy("alice");
        resource.setFolderId(UUID.randomUUID());
        resource.setGroupPermissions(List.of(new ResourceGroupPermission(UUID.randomUUID(), UUID.randomUUID())));
        return resource;
    }
}
