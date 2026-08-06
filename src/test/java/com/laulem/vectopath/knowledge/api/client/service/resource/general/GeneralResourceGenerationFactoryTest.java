package com.laulem.vectopath.knowledge.api.client.service.resource.general;

import com.laulem.vectopath.knowledge.api.client.exception.UnsupportedSourceTypeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneralResourceGenerationFactoryTest {

    @Mock
    private GeneralResourceGeneration textGeneration;

    @Test
    void getResourceGeneration_shouldReturnMatchingGeneration_byInsensitiveCasedSourceType() {
        // Given
        when(textGeneration.getSourceType()).thenReturn("TEXT");
        GeneralResourceGenerationFactory factory = new GeneralResourceGenerationFactory(List.of(textGeneration));

        // When
        GeneralResourceGeneration result = factory.getResourceGeneration("TeXt");

        // Then
        assertThat(result).isSameAs(textGeneration);
    }

    @Test
    void getResourceGeneration_shouldThrowUnsupportedSourceTypeException_whenNoGenerationMatches() {
        // Given
        GeneralResourceGenerationFactory factory = new GeneralResourceGenerationFactory(List.of());

        // When & Then
        assertThatThrownBy(() -> factory.getResourceGeneration("XML"))
                .isInstanceOf(UnsupportedSourceTypeException.class);
    }
}
