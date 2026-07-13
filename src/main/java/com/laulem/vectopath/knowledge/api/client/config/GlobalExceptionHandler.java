package com.laulem.vectopath.knowledge.api.client.config;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.laulem.vectopath.knowledge.api.business.exception.DownloadInterruptedException;
import com.laulem.vectopath.knowledge.api.business.exception.HttpDownloadException;
import com.laulem.vectopath.knowledge.api.business.exception.NotFoundException;
import com.laulem.vectopath.knowledge.api.business.exception.ParamException;
import com.laulem.vectopath.knowledge.api.business.exception.ResourceDeletionException;
import com.laulem.vectopath.knowledge.api.business.exception.VectorizationException;
import com.laulem.vectopath.knowledge.api.client.dto.GeneralResponseException;
import com.laulem.vectopath.knowledge.api.client.exception.UnsupportedFileExtensionException;
import com.laulem.vectopath.knowledge.api.client.exception.UnsupportedSourceTypeException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler(InvalidFormatException.class)
    public ResponseEntity<GeneralResponseException> handleInvalidFormatException(InvalidFormatException ex, HttpServletRequest request) {
        logger.warn("InvalidFormatException: path={}, message={}", request.getRequestURI(), ex.getMessage());
        String field = ex.getPath().isEmpty() ? null : ex.getPath().getLast().getFieldName();
        GeneralResponseException response = new GeneralResponseException(
                "INVALID_FORMAT",
                "Invalid value for field '" + field + "': expected type " + ex.getTargetType().getSimpleName(),
                buildPath(request),
                field,
                Map.of("invalid_value", String.valueOf(ex.getValue()), "expected_type", ex.getTargetType().getSimpleName())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GeneralResponseException> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, HttpServletRequest request) {
        if (ex.getCause() instanceof InvalidFormatException cause) {
            return handleInvalidFormatException(cause, request);
        }
        logger.warn("HttpMessageNotReadableException: path={}, message={}", request.getRequestURI(), ex.getMessage());
        GeneralResponseException response = new GeneralResponseException(
                "INVALID_REQUEST_BODY",
                "Malformed or unreadable JSON request body",
                buildPath(request),
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<GeneralResponseException> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        logger.warn("IllegalArgumentException: path={}, message={}", request.getRequestURI(), ex.getMessage());
        GeneralResponseException response = new GeneralResponseException(
                "INVALID_ARGUMENT",
                ex.getMessage(),
                buildPath(request),
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GeneralResponseException> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String field = ex.getBindingResult().getFieldErrors().isEmpty() ? null
                : ex.getBindingResult().getFieldErrors().getFirst().getField();
        String message = ex.getBindingResult().getFieldErrors().isEmpty() ? ex.getMessage()
                : ex.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();
        logger.warn("MethodArgumentNotValidException: path={}, field={}, message={}", request.getRequestURI(), field, message);
        GeneralResponseException response = new GeneralResponseException(
                "VALIDATION_ERROR",
                message,
                buildPath(request),
                field,
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GeneralResponseException> handleConstraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {
        String message = ex.getConstraintViolations().isEmpty() ? ex.getMessage()
                : ex.getConstraintViolations().iterator().next().getMessage();
        logger.warn("ConstraintViolationException: path={}, message={}", request.getRequestURI(), message);
        GeneralResponseException response = new GeneralResponseException(
                "VALIDATION_ERROR",
                message,
                buildPath(request),
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GeneralResponseException> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        logger.warn("MethodArgumentTypeMismatchException: path={}, message={}", request.getRequestURI(), ex.getMessage());
        GeneralResponseException response = new GeneralResponseException(
                "INVALID_ARGUMENT",
                ex.getMessage(),
                buildPath(request),
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<GeneralResponseException> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        logger.warn("HttpMediaTypeNotSupportedException: path={}, contentType={}", request.getRequestURI(), ex.getContentType());
        GeneralResponseException response = new GeneralResponseException(
                "UNSUPPORTED_MEDIA_TYPE",
                ex.getMessage(),
                buildPath(request),
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<GeneralResponseException> handleHttpRequestMethodNotSupportedException(Exception ex, HttpServletRequest request) {
        logger.warn("HttpRequestMethodNotSupportedException: path={}, message={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<GeneralResponseException> handleAuthorizationDeniedException(AuthorizationDeniedException ex, HttpServletRequest request) {
        logger.warn("AuthorizationDeniedException: path={}, message={}", request.getRequestURI(), ex.getMessage());
        GeneralResponseException response = new GeneralResponseException(
                "ACCESS_DENIED",
                "You do not have permission to access this resource",
                buildPath(request),
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(ParamException.class)
    public ResponseEntity<GeneralResponseException> handleParamException(ParamException ex, HttpServletRequest request) {
        logger.warn("ParamException: code={}, field={}, path={}, message={}",
                ex.getCode(), ex.getField(), request.getRequestURI(), ex.getMessage());
        GeneralResponseException response = new GeneralResponseException(
                ex.getCode(),
                ex.getMessage(),
                buildPath(request),
                ex.getField(),
                ex.getInformation()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<GeneralResponseException> handleNotFoundException(NotFoundException ex, HttpServletRequest request) {
        logger.warn("NotFoundException: path={}, message={}", request.getRequestURI(), ex.getMessage());
        GeneralResponseException response = new GeneralResponseException(
                "NOT_FOUND",
                ex.getMessage(),
                buildPath(request),
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

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
                ex.getMessage(),
                buildPath(request),
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(HttpDownloadException.class)
    public ResponseEntity<GeneralResponseException> handleHttpDownloadException(HttpDownloadException ex, HttpServletRequest request) {
        logger.error("HttpDownloadException: path={}, message={}", request.getRequestURI(), ex.getMessage(), ex);
        GeneralResponseException response = new GeneralResponseException(
                "HTTP_DOWNLOAD_ERROR",
                ex.getMessage(),
                buildPath(request),
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(DownloadInterruptedException.class)
    public ResponseEntity<GeneralResponseException> handleDownloadInterruptedException(DownloadInterruptedException ex, HttpServletRequest request) {
        logger.error("DownloadInterruptedException: path={}, message={}", request.getRequestURI(), ex.getMessage(), ex);
        GeneralResponseException response = new GeneralResponseException(
                "DOWNLOAD_INTERRUPTED",
                ex.getMessage(),
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
                ex.getMessage(),
                buildPath(request),
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GeneralResponseException> handleGenericException(Exception ex, HttpServletRequest request) {
        logger.error("Unexpected exception: path={}, message={}", request.getRequestURI(), ex.getMessage(), ex);
        GeneralResponseException response = new GeneralResponseException(
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                buildPath(request),
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<GeneralResponseException> handleGenericException(NoResourceFoundException ex, HttpServletRequest request) {
        logger.error("Endpoint not found: path={}, message={}", request.getRequestURI(), ex.getMessage(), ex);
        GeneralResponseException response = new GeneralResponseException(
                "NOT_FOUND",
                "Endpoint not found",
                buildPath(request),
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    private String buildPath(HttpServletRequest request) {
        return request.getRequestURI();
    }
}

