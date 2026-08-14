package com.opendatajungle.knowledge.api.client.config;

import com.opendatajungle.commons.client.dto.GeneralResponseException;
import com.opendatajungle.knowledge.api.business.exception.ContentDownloadException;
import com.opendatajungle.knowledge.api.business.exception.HttpDownloadException;
import com.opendatajungle.knowledge.api.business.exception.ResourceDeletionException;
import com.opendatajungle.knowledge.api.business.exception.SemanticSearchException;
import com.opendatajungle.knowledge.api.business.exception.VectorizationException;
import com.opendatajungle.knowledge.api.client.exception.UnsupportedFileExtensionException;
import com.opendatajungle.knowledge.api.client.exception.UnsupportedSourceTypeException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationExceptionHandlerTest {

    private static final String REQUEST_PATH = "/api/v1/resources";

    @Mock
    private HttpServletRequest request;

    private ApplicationExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ApplicationExceptionHandler();
        when(request.getRequestURI()).thenReturn(REQUEST_PATH);
    }

    @Test
    void handleUnsupportedSourceTypeException_shouldReturnBadRequest() {
        // Given
        UnsupportedSourceTypeException ex = new UnsupportedSourceTypeException("XML");

        // When
        ResponseEntity<GeneralResponseException> response = handler.handleUnsupportedSourceTypeException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("UNSUPPORTED_SOURCE_TYPE");
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
    }

    @Test
    void handleUnsupportedFileExtensionException_shouldReturnBadRequest() {
        // Given
        UnsupportedFileExtensionException ex = new UnsupportedFileExtensionException("exe");

        // When
        ResponseEntity<GeneralResponseException> response = handler.handleUnsupportedFileExtensionException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("UNSUPPORTED_FILE_EXTENSION");
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
    }

    @Test
    void handleVectorizationException_shouldReturnInternalServerError() {
        // Given
        VectorizationException ex = new VectorizationException("resource-1", new RuntimeException("boom"));

        // When
        ResponseEntity<GeneralResponseException> response = handler.handleVectorizationException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("VECTORIZATION_ERROR");
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
    }

    @Test
    void handleSemanticSearchException_shouldReturnInternalServerError() {
        // Given
        SemanticSearchException ex = new SemanticSearchException(new RuntimeException("boom"));

        // When
        ResponseEntity<GeneralResponseException> response = handler.handleSemanticSearchException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("SEMANTIC_SEARCH_ERROR");
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
    }

    @Test
    void handleMaxUploadSizeExceededException_shouldReturnContentTooLarge() {
        // Given
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(1_024L);

        // When
        ResponseEntity<GeneralResponseException> response = handler.handleMaxUploadSizeExceededException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
        assertThat(response.getBody().code()).isEqualTo("CONTENT_TOO_LARGE");
        assertThat(response.getBody().field()).isEqualTo("file");
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
    }

    @Test
    void handleHttpDownloadException_shouldReturnBadGateway() {
        // Given
        HttpDownloadException ex = new HttpDownloadException(502, "http://laulem.com");

        // When
        ResponseEntity<GeneralResponseException> response = handler.handleHttpDownloadException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody().code()).isEqualTo("HTTP_DOWNLOAD_ERROR");
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
    }

    @Test
    void handleContentDownloadException_shouldReturnInternalServerError() {
        // Given
        ContentDownloadException ex = new ContentDownloadException("http://laulem.com", new RuntimeException("boom"));

        // When
        ResponseEntity<GeneralResponseException> response = handler.handleContentDownloadException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("DOWNLOAD_INTERRUPTED");
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
    }

    @Test
    void handleVectorStoreDeletionException_shouldReturnInternalServerError() {
        // Given
        ResourceDeletionException ex = new ResourceDeletionException(UUID.randomUUID(), new RuntimeException("boom"));

        // When
        ResponseEntity<GeneralResponseException> response = handler.handleVectorStoreDeletionException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("VECTOR_STORE_DELETION_ERROR");
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
    }
}
