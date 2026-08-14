package com.opendatajungle.knowledge.api.client.service.resource.general;

import com.opendatajungle.commons.business.exception.ParamException;
import com.opendatajungle.knowledge.api.business.model.Resource;
import com.opendatajungle.knowledge.api.business.service.ResourceUseCase;
import com.opendatajungle.knowledge.api.client.dto.CreateResourceRequest;
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
class TextResourceGenerationTest {

    @Mock
    private ResourceUseCase resourceUseCase;

    @InjectMocks
    private TextResourceGeneration generation;

    @Test
    void getSourceType_shouldReturnText() {
        assertThat(generation.getSourceType()).isEqualTo("TEXT");
    }

    @Test
    void processResource_shouldPopulateResourceFromRequest_andDelegateToUseCase() {
        // Given
        UUID folderId = UUID.randomUUID();
        CreateResourceRequest request = new CreateResourceRequest("note", "hello world", null, null, null, folderId, null);
        Resource resource = new Resource();
        resource.setName("note");
        when(resourceUseCase.createResource(any(Resource.class))).thenReturn(resource);

        // When
        Resource result = generation.processResource(resource, request);

        // Then
        assertThat(result.getContent()).isEqualTo("hello world");
        assertThat(result.getSourceType()).isEqualTo("TEXT");
        assertThat(result.getContentType()).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
        assertThat(result.getSize()).isEqualTo("hello world".getBytes().length);
        assertThat(result.getFolderId()).isEqualTo(folderId);
        verify(resourceUseCase).createResource(resource);
    }

    @Test
    void processResource_shouldThrowParamException_whenResourceNameIsBlank() {
        // Given
        CreateResourceRequest request = new CreateResourceRequest(" ", "hello", null, null, null, null, null);
        Resource resource = new Resource();
        resource.setName(" ");

        // When & Then
        assertThatThrownBy(() -> generation.processResource(resource, request))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getField()).isEqualTo("name"));
    }

    @Test
    void processResource_shouldThrowParamException_whenContentIsBlank() {
        // Given
        CreateResourceRequest request = new CreateResourceRequest("note", " ", null, "TEXT", null, null, null);
        Resource resource = new Resource();
        resource.setName("note");

        // When & Then
        assertThatThrownBy(() -> generation.processResource(resource, request))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getField()).isEqualTo("content"));
    }
}
