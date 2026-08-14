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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

// Handles only exceptions specific to this app; cross-cutting HTTP/validation exceptions are handled by commons' GlobalExceptionHandler
@RestControllerAdvice
public class ApplicationExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationExceptionHandler.class);

    @ExceptionHandler(UnsupportedSourceTypeException.class)
    public ResponseEntity<GeneralResponseException> handleUnsupportedSourceTypeException(UnsupportedSourceTypeException ex, HttpServletRequest request) {
        logger.warn("UnsupportedSourceTypeException: path={}, message={}", request.getRequestURI(), ex.getMessage());
        GeneralResponseException response = new GeneralResponseException(
                "UNSUPPORTED_SOURCE_TYPE",
                ex.getMessage(),
                buildPath(request),
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(UnsupportedFileExtensionException.class)
    public ResponseEntity<GeneralResponseException> handleUnsupportedFileExtensionException(UnsupportedFileExtensionException ex, HttpServletRequest request) {
        logger.warn("UnsupportedFileExtensionException: path={}, message={}", request.getRequestURI(), ex.getMessage());
        GeneralResponseException response = new GeneralResponseException(
                "UNSUPPORTED_FILE_EXTENSION",
                ex.getMessage(),
                buildPath(request),
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(VectorizationException.class)
    public ResponseEntity<GeneralResponseException> handleVectorizationException(VectorizationException ex, HttpServletRequest request) {
        logger.error("VectorizationException: path={}, message={}", request.getRequestURI(), ex.getMessage(), ex);
        GeneralResponseException response = new GeneralResponseException(
                "VECTORIZATION_ERROR",
                "The resource could not be vectorized",
                buildPath(request),
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(SemanticSearchException.class)
    public ResponseEntity<GeneralResponseException> handleSemanticSearchException(SemanticSearchException ex, HttpServletRequest request) {
        logger.error("SemanticSearchException: path={}, message={}", request.getRequestURI(), ex.getMessage(), ex);
        GeneralResponseException response = new GeneralResponseException(
                "SEMANTIC_SEARCH_ERROR",
                "The semantic search could not be completed",
                buildPath(request),
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<GeneralResponseException> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        logger.warn("MaxUploadSizeExceededException: path={}", request.getRequestURI());
        GeneralResponseException response = new GeneralResponseException(
                "CONTENT_TOO_LARGE",
                "The uploaded file exceeds the maximum allowed size",
                buildPath(request),
                "file",
                null
        );
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(response);
    }

    @ExceptionHandler(HttpDownloadException.class)
    public ResponseEntity<GeneralResponseException> handleHttpDownloadException(HttpDownloadException ex, HttpServletRequest request) {
        logger.error("HttpDownloadException: path={}, message={}", request.getRequestURI(), ex.getMessage(), ex);
        GeneralResponseException response = new GeneralResponseException(
                "HTTP_DOWNLOAD_ERROR",
                "The remote content could not be downloaded",
                buildPath(request),
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    @ExceptionHandler(ContentDownloadException.class)
    public ResponseEntity<GeneralResponseException> handleContentDownloadException(ContentDownloadException ex, HttpServletRequest request) {
        logger.error("ContentDownloadException: path={}, message={}", request.getRequestURI(), ex.getMessage(), ex);
        GeneralResponseException response = new GeneralResponseException(
                "DOWNLOAD_INTERRUPTED",
                "The remote content download was interrupted",
                buildPath(request),
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(ResourceDeletionException.class)
    public ResponseEntity<GeneralResponseException> handleVectorStoreDeletionException(ResourceDeletionException ex, HttpServletRequest request) {
        logger.error("VectorStoreDeletionException: path={}, message={}", request.getRequestURI(), ex.getMessage(), ex);
        GeneralResponseException response = new GeneralResponseException(
                "VECTOR_STORE_DELETION_ERROR",
                "The resource could not be deleted",
                buildPath(request),
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String buildPath(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
