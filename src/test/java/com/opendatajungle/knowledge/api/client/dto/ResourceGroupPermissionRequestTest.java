package com.opendatajungle.knowledge.api.client.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceGroupPermissionRequestTest {

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
    void validate_shouldReportNoViolations_whenBothIdsArePresent() {
        // Given
        ResourceGroupPermissionRequest request = new ResourceGroupPermissionRequest(UUID.randomUUID(), UUID.randomUUID());

        // When
        Set<ConstraintViolation<ResourceGroupPermissionRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void validate_shouldReportOneViolationPerMissingId_whenBothAreNull() {
        // Given
        ResourceGroupPermissionRequest request = new ResourceGroupPermissionRequest(null, null);

        // When
        Set<ConstraintViolation<ResourceGroupPermissionRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(2);
        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("groupId", "permissionId");
    }
}
