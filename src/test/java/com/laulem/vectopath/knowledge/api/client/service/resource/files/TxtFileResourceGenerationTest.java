package com.laulem.vectopath.knowledge.api.client.service.resource.files;

import com.laulem.vectopath.knowledge.api.business.exception.ParamException;
import com.laulem.vectopath.knowledge.api.business.model.Resource;
import com.laulem.vectopath.knowledge.api.business.service.ResourceUseCase;
import com.laulem.vectopath.knowledge.api.client.dto.CreateResourceRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TxtFileResourceGenerationTest {

    @Mock
    private ResourceUseCase resourceUseCase;

    @InjectMocks
    private TxtFileResourceGeneration generation;

    @Test
    void getFileExtension_shouldReturnTxt() {
        assertThat(generation.getFileExtension()).isEqualTo("TXT");
    }

    @Test
    void processResource_shouldPopulateResourceFromFile_andDelegateToUseCase() throws Exception {
        // Given
        UUID folderId = UUID.randomUUID();
        MultipartFile file = new MockMultipartFile("file", "../secret/notes.txt", null, "hello world".getBytes());
        CreateResourceRequest request = new CreateResourceRequest("notes", null, null, "file", null, folderId, null);
        Resource resource = new Resource();
        resource.setName("notes");
        when(resourceUseCase.createResource(any(Resource.class))).thenReturn(resource);

        // When
        Resource result = generation.processResource(resource, request, file);

        // Then
        assertThat(result.getContent()).isEqualTo("hello world");
        assertThat(result.getSourceType()).isEqualTo("FILE");
        assertThat(result.getSourceName()).isEqualTo("notes.txt");
        assertThat(result.getContentType()).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
        assertThat(result.getSize()).isEqualTo(file.getSize());
        assertThat(result.getFolderId()).isEqualTo(folderId);
        verify(resourceUseCase).createResource(resource);
    }

    @Test
    void processResource_shouldSanitizeFileName_whenOriginalFilenameContainsWindowsPath() throws Exception {
        // Given
        MultipartFile file = new MockMultipartFile("file", "C:\\Windows\\System32\\evil.txt", null, "hello".getBytes());
        CreateResourceRequest request = new CreateResourceRequest("notes", null, null, "file", null, null, null);
        Resource resource = new Resource();
        resource.setName("notes");
        when(resourceUseCase.createResource(any(Resource.class))).thenReturn(resource);

        // When
        Resource result = generation.processResource(resource, request, file);

        // Then
        assertThat(result.getSourceName()).isEqualTo("evil.txt");
    }

    @Test
    void processResource_shouldThrowParamException_whenOriginalFilenameIsNull() {
        // Given
        MultipartFile file = new MockMultipartFile("file", null, null, "hello".getBytes());
        CreateResourceRequest request = new CreateResourceRequest("notes", null, null, "file", null, null, null);
        Resource resource = new Resource();
        resource.setName("notes");

        // When & Then
        assertThatThrownBy(() -> generation.processResource(resource, request, file))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getField()).isEqualTo("file"));
    }

    @Test
    void processResource_shouldThrowParamException_whenOriginalFilenameIsBlank() {
        // Given
        MultipartFile file = new MockMultipartFile("file", " ", null, "hello".getBytes());
        CreateResourceRequest request = new CreateResourceRequest("notes", null, null, "file", null, null, null);
        Resource resource = new Resource();
        resource.setName("notes");

        // When & Then
        assertThatThrownBy(() -> generation.processResource(resource, request, file))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getField()).isEqualTo("file"));
    }

    @Test
    void processResource_shouldThrowParamException_whenSanitizedFileNameIsBlank() {
        // Given
        MultipartFile file = new MockMultipartFile("file", " / ", null, "hello".getBytes());
        CreateResourceRequest request = new CreateResourceRequest("notes", null, null, "file", null, null, null);
        Resource resource = new Resource();
        resource.setName("notes");

        // When & Then
        assertThatThrownBy(() -> generation.processResource(resource, request, file))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getField()).isEqualTo("file"));
    }

    @Test
    void processResource_shouldThrowParamException_whenResourceNameIsBlank() {
        // Given
        MultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());
        CreateResourceRequest request = new CreateResourceRequest(" ", null, null, "file", null, null, null);
        Resource resource = new Resource();
        resource.setName(" ");

        // When & Then
        assertThatThrownBy(() -> generation.processResource(resource, request, file))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getField()).isEqualTo("name"));
    }

    @Test
    void processResource_shouldThrowParamException_whenFileContentIsBlank() {
        // Given
        MultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", " ".getBytes());
        CreateResourceRequest request = new CreateResourceRequest("notes", null, null, "file", null, null, null);
        Resource resource = new Resource();
        resource.setName("notes");

        // When & Then
        assertThatThrownBy(() -> generation.processResource(resource, request, file))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getField()).isEqualTo("file"));
    }
}
