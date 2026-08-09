package com.opendatajungle.knowledge.api.business.service.splitter;

import com.opendatajungle.knowledge.api.business.model.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentSplitterFactoryTest {

    @Mock
    private DocumentSplitter defaultSplitter;

    @Mock
    private DocumentSplitter fileTxtSplitter;

    @Mock
    private DocumentSplitter fileSplitter;

    @Mock
    private DocumentSplitter urlSplitter;

    @Test
    void getSplitter_shouldReturnDefault_whenSourceTypeIsNull() {
        // Given
        when(defaultSplitter.getSplitterKey()).thenReturn("DEFAULT");
        DocumentSplitterFactory factory = new DocumentSplitterFactory(List.of(defaultSplitter));
        Resource resource = new Resource();

        // When
        DocumentSplitter result = factory.getSplitter(resource);

        // Then
        assertThat(result).isSameAs(defaultSplitter);
    }

    @Test
    void getSplitter_shouldReturnMatchingSourceTypeSplitter() {
        // Given
        when(defaultSplitter.getSplitterKey()).thenReturn("DEFAULT");
        when(urlSplitter.getSplitterKey()).thenReturn("URL");
        DocumentSplitterFactory factory = new DocumentSplitterFactory(List.of(defaultSplitter, urlSplitter));
        Resource resource = new Resource();
        resource.setSourceType("url");

        // When
        DocumentSplitter result = factory.getSplitter(resource);

        // Then
        assertThat(result).isSameAs(urlSplitter);
    }

    @Test
    void getSplitter_shouldPreferFileExtensionSpecificSplitter_overGenericFileSplitter() {
        // Given
        when(defaultSplitter.getSplitterKey()).thenReturn("DEFAULT");
        when(fileSplitter.getSplitterKey()).thenReturn("FILE");
        when(fileTxtSplitter.getSplitterKey()).thenReturn("FILE_TXT");
        DocumentSplitterFactory factory = new DocumentSplitterFactory(List.of(defaultSplitter, fileSplitter, fileTxtSplitter));
        Resource resource = new Resource();
        resource.setSourceType("file");
        resource.setSourceName("notes.txt");

        // When
        DocumentSplitter result = factory.getSplitter(resource);

        // Then
        assertThat(result).isSameAs(fileTxtSplitter);
    }

    @Test
    void getSplitter_shouldFallBackToGenericFileSplitter_whenNoExtensionSpecificSplitterRegistered() {
        // Given
        when(defaultSplitter.getSplitterKey()).thenReturn("DEFAULT");
        when(fileSplitter.getSplitterKey()).thenReturn("FILE");
        DocumentSplitterFactory factory = new DocumentSplitterFactory(List.of(defaultSplitter, fileSplitter));
        Resource resource = new Resource();
        resource.setSourceType("file");
        resource.setSourceName("archive.tar.gz");

        // When
        DocumentSplitter result = factory.getSplitter(resource);

        // Then
        assertThat(result).isSameAs(fileSplitter);
    }

    @Test
    void getSplitter_shouldFallBackToGenericFileSplitter_whenSourceNameIsNull() {
        // Given
        when(defaultSplitter.getSplitterKey()).thenReturn("DEFAULT");
        when(fileSplitter.getSplitterKey()).thenReturn("FILE");
        DocumentSplitterFactory factory = new DocumentSplitterFactory(List.of(defaultSplitter, fileSplitter));
        Resource resource = new Resource();
        resource.setSourceType("file");

        // When
        DocumentSplitter result = factory.getSplitter(resource);

        // Then
        assertThat(result).isSameAs(fileSplitter);
    }

    @Test
    void getSplitter_shouldFallBackToGenericFileSplitter_whenSourceNameHasNoExtension() {
        // Given
        when(defaultSplitter.getSplitterKey()).thenReturn("DEFAULT");
        when(fileSplitter.getSplitterKey()).thenReturn("FILE");
        DocumentSplitterFactory factory = new DocumentSplitterFactory(List.of(defaultSplitter, fileSplitter));
        Resource resource = new Resource();
        resource.setSourceType("file");
        resource.setSourceName("README");

        // When
        DocumentSplitter result = factory.getSplitter(resource);

        // Then
        assertThat(result).isSameAs(fileSplitter);
    }

    @Test
    void getSplitter_shouldFallBackToGenericFileSplitter_whenSourceNameEndsWithDot() {
        // Given
        when(defaultSplitter.getSplitterKey()).thenReturn("DEFAULT");
        when(fileSplitter.getSplitterKey()).thenReturn("FILE");
        DocumentSplitterFactory factory = new DocumentSplitterFactory(List.of(defaultSplitter, fileSplitter));
        Resource resource = new Resource();
        resource.setSourceType("file");
        resource.setSourceName("archive.");

        // When
        DocumentSplitter result = factory.getSplitter(resource);

        // Then
        assertThat(result).isSameAs(fileSplitter);
    }

    @Test
    void getSplitter_shouldMatchExtensionSpecificSplitter_regardlessOfSourceNameCase() {
        // Given
        when(defaultSplitter.getSplitterKey()).thenReturn("DEFAULT");
        when(fileSplitter.getSplitterKey()).thenReturn("FILE");
        when(fileTxtSplitter.getSplitterKey()).thenReturn("FILE_TXT");
        DocumentSplitterFactory factory = new DocumentSplitterFactory(List.of(defaultSplitter, fileSplitter, fileTxtSplitter));
        Resource resource = new Resource();
        resource.setSourceType("file");
        resource.setSourceName("notes.TxT");

        // When
        DocumentSplitter result = factory.getSplitter(resource);

        // Then
        assertThat(result).isSameAs(fileTxtSplitter);
    }

    @Test
    void getSplitter_shouldFallBackToDefaultSplitter_whenNoExtensionSpecificSplitterRegistered() {
        // Given
        when(defaultSplitter.getSplitterKey()).thenReturn("DEFAULT");
        when(fileSplitter.getSplitterKey()).thenReturn("FILE_TXT");
        DocumentSplitterFactory factory = new DocumentSplitterFactory(List.of(defaultSplitter, fileSplitter));
        Resource resource = new Resource();
        resource.setSourceType("file");
        resource.setSourceName("archive.tar.gz");

        // When
        DocumentSplitter result = factory.getSplitter(resource);

        // Then
        assertThat(result).isSameAs(defaultSplitter);
    }

    @Test
    void getSplitter_shouldFallBackToDefault_whenSourceTypeHasNoRegisteredSplitter() {
        // Given
        when(defaultSplitter.getSplitterKey()).thenReturn("DEFAULT");
        DocumentSplitterFactory factory = new DocumentSplitterFactory(List.of(defaultSplitter));
        Resource resource = new Resource();
        resource.setSourceType("url");

        // When
        DocumentSplitter result = factory.getSplitter(resource);

        // Then
        assertThat(result).isSameAs(defaultSplitter);
    }
}
