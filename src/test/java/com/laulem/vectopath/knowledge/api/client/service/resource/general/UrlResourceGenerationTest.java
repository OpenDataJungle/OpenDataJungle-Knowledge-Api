package com.laulem.vectopath.knowledge.api.client.service.resource.general;

import com.laulem.vectopath.knowledge.api.business.exception.ParamException;
import com.laulem.vectopath.knowledge.api.business.model.Resource;
import com.laulem.vectopath.knowledge.api.business.service.ContentDownloaderUseCase;
import com.laulem.vectopath.knowledge.api.business.service.ResourceUseCase;
import com.laulem.vectopath.knowledge.api.client.dto.CreateResourceRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlResourceGenerationTest {

    @Mock
    private ResourceUseCase resourceUseCase;

    @Mock
    private ContentDownloaderUseCase contentDownloaderUseCase;

    @InjectMocks
    private UrlResourceGeneration generation;

    @Test
    void getSourceType_shouldReturnUrl() {
        assertThat(generation.getSourceType()).isEqualTo("URL");
    }

    @Test
    void processResource_shouldDownloadContent_populateResource_andDelegateToUseCase() throws Exception {
        // Given
        UUID folderId = UUID.randomUUID();
        CreateResourceRequest request = new CreateResourceRequest("page", null, "  https://laulem.com  ", null, null, folderId, null);
        Resource resource = new Resource();
        resource.setName("page");
        when(contentDownloaderUseCase.downloadContent("https://laulem.com")).thenReturn("downloaded content");
        when(resourceUseCase.createResource(any(Resource.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Resource result = generation.processResource(resource, request);

        // Then
        assertThat(result.getContent()).isEqualTo("downloaded content");
        assertThat(result.getSourceType()).isEqualTo("URL");
        assertThat(result.getSourceName()).isEqualTo("  https://laulem.com  ");
        assertThat(result.getContentType()).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
        assertThat(result.getSize()).isEqualTo("downloaded content".getBytes().length);
        assertThat(result.getFolderId()).isEqualTo(folderId);
        verify(contentDownloaderUseCase).downloadContent("https://laulem.com");
        verify(resourceUseCase).createResource(resource);
    }

    @Test
    void processResource_shouldThrowParamException_whenResourceNameIsBlank() {
        // Given
        CreateResourceRequest request = new CreateResourceRequest(" ", null, "https://laulem.com", "URL", null, null, null);
        Resource resource = new Resource();
        resource.setName(" ");

        // When & Then
        assertThatThrownBy(() -> generation.processResource(resource, request))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getField()).isEqualTo("name"));
    }

    @Test
    void processResource_shouldThrowParamException_whenUrlIsBlank() {
        // Given
        CreateResourceRequest request = new CreateResourceRequest("page", null, " ", "URL", null, null, null);
        Resource resource = new Resource();
        resource.setName("page");

        // When & Then
        assertThatThrownBy(() -> generation.processResource(resource, request))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getField()).isEqualTo("url"));
    }
}
