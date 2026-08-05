package com.laulem.vectopath.knowledge.api.client.service;

import com.laulem.vectopath.knowledge.api.business.exception.ParamException;
import com.laulem.vectopath.knowledge.api.business.model.Resource;
import com.laulem.vectopath.knowledge.api.business.service.AuthenticationUseCase;
import com.laulem.vectopath.knowledge.api.client.dto.CreateResourceRequest;
import com.laulem.vectopath.knowledge.api.client.dto.ResourceGroupPermissionRequest;
import com.laulem.vectopath.knowledge.api.client.service.resource.files.FileResourceGeneration;
import com.laulem.vectopath.knowledge.api.client.service.resource.general.GeneralResourceGeneration;
import com.laulem.vectopath.knowledge.api.client.service.resource.general.GeneralResourceGenerationFactory;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceCreationServiceTest {

    @Mock
    private GeneralResourceGenerationFactory generalFactory;

    @Mock
    private AuthenticationUseCase authenticationUseCase;

    @Mock
    private GeneralResourceGeneration generalResourceGeneration;

    @InjectMocks
    private ResourceCreationService service;

    @Test
    void createGeneralResource_shouldUseDefaultTextSourceType_whenNoneProvided() throws Exception {
        // Given
        CreateResourceRequest request = new CreateResourceRequest("name", "content", null, null, null, null, null);
        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");
        when(generalFactory.getResourceGeneration("TEXT")).thenReturn(generalResourceGeneration);
        Resource generated = new Resource();
        when(generalResourceGeneration.processResource(any(Resource.class), eq(request))).thenReturn(generated);

        // When
        Resource result = service.createGeneralResource(request);

        // Then
        assertThat(result).isSameAs(generated);
        verify(generalFactory).getResourceGeneration("TEXT");
    }

    @Test
    void createGeneralResource_shouldUppercaseProvidedSourceType() throws Exception {
        // Given
        CreateResourceRequest request = new CreateResourceRequest("name", null, "http://example.com", "url", null, null, null);
        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");
        when(generalFactory.getResourceGeneration("URL")).thenReturn(generalResourceGeneration);
        when(generalResourceGeneration.processResource(any(Resource.class), eq(request))).thenReturn(new Resource());

        // When
        service.createGeneralResource(request);

        // Then
        verify(generalFactory).getResourceGeneration("URL");
    }

    @Test
    void createGeneralResource_shouldBuildBaseResourceWithCurrentUserAndGroupPermissions() throws Exception {
        // Given
        UUID groupId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();
        CreateResourceRequest request = new CreateResourceRequest(
                "name", "content", null, "TEXT", "meta", null,
                List.of(new ResourceGroupPermissionRequest(groupId, permissionId)));
        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");
        when(generalFactory.getResourceGeneration("TEXT")).thenReturn(generalResourceGeneration);
        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        Resource generated = new Resource();
        when(generalResourceGeneration.processResource(resourceCaptor.capture(), eq(request))).thenReturn(generated);

        // When
        Resource result = service.createGeneralResource(request);

        // Then
        assertThat(result).isSameAs(generated);
        Resource captured = resourceCaptor.getValue();
        assertThat(captured.getName()).isEqualTo("name");
        assertThat(captured.getMetadata()).isEqualTo("meta");
        assertThat(captured.getCreatedBy()).isEqualTo("alice");
        assertThat(captured.getGroupPermissions()).hasSize(1);
        assertThat(captured.getGroupPermissions().getFirst().getGroupId()).isEqualTo(groupId);
        assertThat(captured.getGroupPermissions().getFirst().getPermissionId()).isEqualTo(permissionId);
    }

    @Test
    void createFileResource_shouldThrowParamException_whenFileIsEmpty() {
        // Given
        MultipartFile emptyFile = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);
        CreateResourceRequest request = new CreateResourceRequest("name", null, null, "file", null, null, null);

        // When & Then
        assertThatThrownBy(() -> service.createFileResource(request, emptyFile))
                .asInstanceOf(InstanceOfAssertFactories.type(ParamException.class))
                .extracting(ParamException::getField)
                .isEqualTo("file");
    }

    @Test
    void createFileResource_shouldThrowParamException_whenFileIsNull() {
        // Given
        CreateResourceRequest request = new CreateResourceRequest("name", null, null, "file", null, null, null);

        // When & Then
        assertThatThrownBy(() -> service.createFileResource(request, null))
                .asInstanceOf(InstanceOfAssertFactories.type(ParamException.class))
                .extracting(ParamException::getField)
                .isEqualTo("file");
    }

    @Test
    void createGeneralResource_shouldMapGroupPermissionsToEmptyList_whenNoneProvided() throws Exception {
        // Given
        CreateResourceRequest request = new CreateResourceRequest("name", "content", null, "TEXT", null, null, null);
        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");
        when(generalFactory.getResourceGeneration("TEXT")).thenReturn(generalResourceGeneration);
        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        Resource generated = new Resource();
        when(generalResourceGeneration.processResource(resourceCaptor.capture(), eq(request))).thenReturn(generated);

        // When
        Resource result = service.createGeneralResource(request);

        // Then
        assertThat(result).isSameAs(generated);
        assertThat(resourceCaptor.getValue().getGroupPermissions()).isEmpty();
    }
}
