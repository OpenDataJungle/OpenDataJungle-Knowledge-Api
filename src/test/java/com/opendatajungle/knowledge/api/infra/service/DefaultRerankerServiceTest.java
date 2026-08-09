package com.opendatajungle.knowledge.api.infra.service;

import com.opendatajungle.knowledge.api.business.model.PartialResource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRerankerServiceTest {

    private final DefaultRerankerService service = new DefaultRerankerService();

    @Test
    void rerank_shouldReturnCandidatesSortedByScoreDescending_andLimitedToRequestedSize() {
        // Given
        PartialResource low = candidate(0.1);
        PartialResource high = candidate(0.9);
        PartialResource medium = candidate(0.5);

        // When
        List<PartialResource> result = service.rerank("query", List.of(low, high, medium), 2);

        // Then
        assertThat(result).containsExactly(high, medium);
    }

    @Test
    void rerank_shouldReturnAllCandidatesSorted_whenLimitExceedsCandidateCount() {
        // Given
        PartialResource low = candidate(0.2);
        PartialResource high = candidate(0.8);

        // When
        List<PartialResource> result = service.rerank("query", List.of(low, high), 10);

        // Then
        assertThat(result).containsExactly(high, low);
    }

    @Test
    void rerank_shouldReturnEmptyList_whenCandidatesIsEmpty() {
        // When
        List<PartialResource> result = service.rerank("query", List.of(), 5);

        // Then
        assertThat(result).isEmpty();
    }

    private static PartialResource candidate(double similarityScore) {
        PartialResource partialResource = new PartialResource();
        partialResource.setSimilarityScore(similarityScore);
        return partialResource;
    }
}
