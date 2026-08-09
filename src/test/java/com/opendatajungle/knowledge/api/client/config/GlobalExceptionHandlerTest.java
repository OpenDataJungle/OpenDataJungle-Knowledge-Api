package com.opendatajungle.knowledge.api.client.config;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.opendatajungle.knowledge.api.business.exception.ContentDownloadException;
import com.opendatajungle.knowledge.api.business.exception.HttpDownloadException;
import com.opendatajungle.knowledge.api.business.exception.NotFoundException;
import com.opendatajungle.knowledge.api.business.exception.ParamException;
import com.opendatajungle.knowledge.api.business.exception.ResourceDeletionException;
import com.opendatajungle.knowledge.api.business.exception.SemanticSearchException;
import com.opendatajungle.knowledge.api.business.exception.VectorizationException;
import com.opendatajungle.knowledge.api.client.dto.GeneralResponseException;
import com.opendatajungle.knowledge.api.client.exception.UnsupportedFileExtensionException;
import com.opendatajungle.knowledge.api.client.exception.UnsupportedSourceTypeException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private static final String REQUEST_PATH = "/api/v1/resources";

    @Mock
    private HttpServletRequest request;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn(REQUEST_PATH);
    }

    @Test
    void handleInvalidFormatException_shouldExposeFieldName_whenPathIsNotEmpty() {
        // Given
        InvalidFormatException ex = new InvalidFormatException("not a number", "abc", Integer.class);
        ex.prependPath(new Object(), "size");

        // When
        ResponseEntity<GeneralResponseException> response = handler.handleInvalidFormatException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("INVALID_FORMAT");
        assertThat(response.getBody().field()).isEqualTo("size");
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
        assertThat(response.getBody().information())
                .containsEntry("invalid_value", "abc")
                .containsEntry("expected_type", "Integer");
    }

    @Test
    void handleInvalidFormatException_shouldReturnNullField_whenPathIsEmpty() {
        // Given
        InvalidFormatException ex = new InvalidFormatException("not a number", "abc", Integer.class);

        // When
        ResponseEntity<GeneralResponseException> response = handler.handleInvalidFormatException(ex, request);

        // Then
        assertThat(response.getBody().field()).isNull();
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
    }

    @Test
    void handleHttpMessageNotReadableException_shouldDelegateToInvalidFormatHandler_whenCauseIsInvalidFormatException() {
        // Given
        InvalidFormatException cause = new InvalidFormatException("not a number", "abc", Integer.class);
        cause.prependPath(new Object(), "size");
        org.springframework.http.converter.HttpMessageNotReadableException ex =
                new org.springframework.http.converter.HttpMessageNotReadableException("unreadable", cause, null);

        // When
        ResponseEntity<GeneralResponseException> response = handler.handleHttpMessageNotReadableException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("INVALID_FORMAT");
        assertThat(response.getBody().field()).isEqualTo("size");
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
    }

    @Test
    void handleHttpMessageNotReadableException_shouldReturnGenericMessage_whenCauseIsNotInvalidFormatException() {
        // Given
        org.springframework.http.converter.HttpMessageNotReadableException ex =
                new org.springframework.http.converter.HttpMessageNotReadableException("malformed json", new RuntimeException("boom"), null);

        // When
        ResponseEntity<GeneralResponseException> response = handler.handleHttpMessageNotReadableException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("INVALID_REQUEST_BODY");
        assertThat(response.getBody().message()).isEqualTo("Malformed or unreadable JSON request body");
        assertThat(response.getBody().field()).isNull();
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
    }

    @Test
    void handleIllegalArgumentException_shouldReturnBadRequest() {
        // Given
        IllegalArgumentException ex = new IllegalArgumentException("bad argument");

        // When
        ResponseEntity<GeneralResponseException> response = handler.handleIllegalArgumentException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("INVALID_ARGUMENT");
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
    }

    @Test
    void handleMethodArgumentNotValidException_shouldExposeFirstFieldError_whenErrorsArePresent() throws Exception {
        // Given
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "name", "must not be blank"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(dummyMethodParameter(), bindingResult);

        // When
        ResponseEntity<GeneralResponseException> response = handler.handleMethodArgumentNotValidException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().field()).isEqualTo("name");
        assertThat(response.getBody().message()).isEqualTo("must not be blank");
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
    }

    @Test
    void handleMethodArgumentNotValidException_shouldFallBackToExceptionMessage_whenNoFieldErrors() throws Exception {
        // Given
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(dummyMethodParameter(), bindingResult);

        // When
        ResponseEntity<GeneralResponseException> response = handler.handleMethodArgumentNotValidException(ex, request);

        // Then
        assertThat(response.getBody().field()).isNull();
        assertThat(response.getBody().message()).isEqualTo(ex.getMessage());
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
    }

    @Test
    void handleConstraintViolationException_shouldExposeFirstViolationMessage_whenViolationsArePresent() {
        // Given
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("must not be null");
        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        // When
        ResponseEntity<GeneralResponseException> response = handler.handleConstraintViolationException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().message()).isEqualTo("must not be null");
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
    }

    @Test
    void handleConstraintViolationException_shouldFallBackToExceptionMessage_whenNoViolations() {
        // Given
        ConstraintViolationException ex = new ConstraintViolationException("no violations detail", Set.of());

        // When
        ResponseEntity<GeneralResponseException> response = handler.handleConstraintViolationException(ex, request);

        // Then
        assertThat(response.getBody().message()).isEqualTo("no violations detail");
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
    }

    @Test
    void handleMethodArgumentTypeMismatchException_shouldExposeParameterName() {
        // Given
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "not-a-uuid", UUID.class, "id", mock(MethodParameter.class), new IllegalArgumentException());

        // When
        ResponseEntity<GeneralResponseException> response = handler.handleMethodArgumentTypeMismatchException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("INVALID_ARGUMENT");
        assertThat(response.getBody().field()).isEqualTo("id");
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
    }

    @Test
    void handleHttpMediaTypeNotSupportedException_shouldReturnUnsupportedMediaType() {
        // Given
        HttpMediaTypeNotSupportedException ex =
                new HttpMediaTypeNotSupportedException(MediaType.APPLICATION_XML, List.of(MediaType.APPLICATION_JSON));

        // When
        ResponseEntity<GeneralResponseException> response = handler.handleHttpMediaTypeNotSupportedException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody().code()).isEqualTo("UNSUPPORTED_MEDIA_TYPE");
        assertThat(response.getBody().message()).isEqualTo(ex.getMessage());
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
    }

    @Test
    void handleMissingServletRequestPartException_shouldExposePartName() {
        // Given
        MissingServletRequestPartException ex = new MissingServletRequestPartException("file");

        // When
        ResponseEntity<GeneralResponseException> response = handler.handleMissingServletRequestPartException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("MISSING_REQUEST_PART");
        assertThat(response.getBody().field()).isEqualTo("file");
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
    }

    @Test
    void handleHttpRequestMethodNotSupportedException_shouldReturnNotFoundWithoutBody() {
        // Given
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("PUT");

        // When
        ResponseEntity<GeneralResponseException> response = handler.handleHttpRequestMethodNotSupportedException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void handleAuthorizationDeniedException_shouldReturnForbidden() {
        // Given
        AuthorizationDeniedException ex = new AuthorizationDeniedException("denied");

        // When
        ResponseEntity<GeneralResponseException> response = handler.handleAuthorizationDeniedException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().code()).isEqualTo("ACCESS_DENIED");
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
    }

    @Test
    void handleParamException_shouldPropagateCodeFieldAndInformation() {
        // Given
        ParamException ex = new ParamException("REQUIRED", "name is required", "name", Map.of("hint", "provide a name"));

        // When
        ResponseEntity<GeneralResponseException> response = handler.handleParamException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("REQUIRED");
        assertThat(response.getBody().field()).isEqualTo("name");
        assertThat(response.getBody().information()).containsEntry("hint", "provide a name");
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
    }

    @Test
    void handleNotFoundException_shouldReturnNotFound() {
        // Given
        NotFoundException ex = new NotFoundException("Resource", "123");

        // When
        ResponseEntity<GeneralResponseException> response = handler.handleNotFoundException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo(ex.getMessage());
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
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

    @Test
    void handleGenericException_shouldReturnInternalServerError() {
        // Given
        Exception ex = new Exception("unexpected");

        // When
        ResponseEntity<GeneralResponseException> response = handler.handleGenericException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
    }

    @Test
    void handleGenericException_shouldReturnNotFound_whenNoResourceFound() {
        // Given
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/unknown", "no static resource unknown");

        // When
        ResponseEntity<GeneralResponseException> response = handler.handleGenericException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
    }

    private static MethodParameter dummyMethodParameter() throws NoSuchMethodException {
        return new MethodParameter(GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyTarget", String.class), 0);
    }

    private static void dummyTarget(String value) {
        // used only via reflection to build a MethodParameter for tests
    }
}
