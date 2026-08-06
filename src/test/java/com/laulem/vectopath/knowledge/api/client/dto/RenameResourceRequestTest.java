package com.laulem.vectopath.knowledge.api.client.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RenameResourceRequestTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void validate_shouldReportNoViolations_whenNameIsPresent() {
        // Given
        RenameResourceRequest request = new RenameResourceRequest("new-name");

        // When
        Set<ConstraintViolation<RenameResourceRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void validate_shouldReportViolation_whenNameIsBlank() {
        // Given
        RenameResourceRequest request = new RenameResourceRequest(" ");

        // When
        Set<ConstraintViolation<RenameResourceRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Name must not be blank");
    }

    @Test
    void validate_shouldReportViolation_whenNameIsNull() {
        // Given
        RenameResourceRequest request = new RenameResourceRequest(null);

        // When
        Set<ConstraintViolation<RenameResourceRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
    }
}
