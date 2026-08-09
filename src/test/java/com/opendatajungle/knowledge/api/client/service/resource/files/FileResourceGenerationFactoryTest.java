package com.opendatajungle.knowledge.api.client.service.resource.files;

import com.opendatajungle.knowledge.api.client.exception.UnsupportedFileExtensionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileResourceGenerationFactoryTest {

    @Mock
    private FileResourceGeneration txtGeneration;

    @Test
    void getResourceGeneration_shouldMatchExtensionCaseInsensitively() {
        // Given
        when(txtGeneration.getFileExtension()).thenReturn("TXT");
        FileResourceGenerationFactory factory = new FileResourceGenerationFactory(List.of(txtGeneration));
        MultipartFile file = new MockMultipartFile("file", "notes.TxT", "text/plain", "content".getBytes());

        // When
        FileResourceGeneration result = factory.getResourceGeneration(file);

        // Then
        assertThat(result).isSameAs(txtGeneration);
    }

    @Test
    void getResourceGeneration_shouldThrowUnsupportedFileExtensionException_whenNoGenerationMatches() {
        // Given
        FileResourceGenerationFactory factory = new FileResourceGenerationFactory(List.of());
        MultipartFile file = new MockMultipartFile("file", "notes.pdf", "application/pdf", "content".getBytes());

        // When & Then
        assertThatThrownBy(() -> factory.getResourceGeneration(file))
                .isInstanceOf(UnsupportedFileExtensionException.class);
    }

    @Test
    void getResourceGeneration_shouldThrowUnsupportedFileExtensionException_whenFileHasNoExtension() {
        // Given
        FileResourceGenerationFactory factory = new FileResourceGenerationFactory(List.of());
        MultipartFile file = new MockMultipartFile("file", "notes", "text/plain", "content".getBytes());

        // When & Then
        assertThatThrownBy(() -> factory.getResourceGeneration(file))
                .isInstanceOf(UnsupportedFileExtensionException.class);
    }

    @Test
    void getResourceGeneration_shouldThrowNullPointerException_whenOriginalFilenameIsNull() {
        // Given
        FileResourceGenerationFactory factory = new FileResourceGenerationFactory(List.of());
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> factory.getResourceGeneration(file))
                .isInstanceOf(NullPointerException.class);
    }
}
