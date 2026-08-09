package com.opendatajungle.knowledge.api.infra.service;

import com.opendatajungle.knowledge.api.business.model.PartialResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
class HuggingFaceRerankerServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private HuggingFaceRerankerService service;

    @BeforeEach
    void setUp() {
        service = new HuggingFaceRerankerService(restClient);
    }

    private void stubExchange() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/rerank")).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(HuggingFaceRerankerService.RerankRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void rerank_shouldReturnSortedCandidates_withoutCallingRestClient_whenCandidatesIsEmpty() {
        // When
        List<PartialResource> result = service.rerank("query", List.of(), 5);

        // Then
        assertThat(result).isEmpty();
        verifyNoInteractions(restClient);
    }

    @Test
    void rerank_shouldReturnSortedCandidates_withoutCallingRestClient_whenCandidatesSizeIsBelowLimit() {
        // Given
        PartialResource low = candidate("low", 0.2);
        PartialResource high = candidate("high", 0.8);

        // When
        List<PartialResource> result = service.rerank("query", List.of(low, high), 5);

        // Then
        assertThat(result).containsExactly(high, low);
        verifyNoInteractions(restClient);
    }

    @Test
    void rerank_shouldCallRestClientAndApplyScores_whenCandidatesSizeMeetsLimit() {
        // Given
        PartialResource first = candidate("first", 0.3);
        PartialResource second = candidate("second", 0.2);
        stubExchange();
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(List.of(
                new HuggingFaceRerankerService.RerankResponse(1, 0.95),
                new HuggingFaceRerankerService.RerankResponse(0, 0.15)));

        // When
        List<PartialResource> result = service.rerank("query", List.of(first, second), 2);

        // Then
        assertThat(result).containsExactly(second, first);
        assertThat(second.getSimilarityScore()).isEqualTo(0.95);
        assertThat(first.getSimilarityScore()).isEqualTo(0.15);
    }

    @Test
    void rerank_shouldSendQueryAndCandidateContentsInRequestBody() {
        // Given
        PartialResource first = candidate("first content", 0.1);
        PartialResource second = candidate("second content", 0.2);
        ArgumentCaptor<HuggingFaceRerankerService.RerankRequest> bodyCaptor =
                ArgumentCaptor.forClass(HuggingFaceRerankerService.RerankRequest.class);
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/rerank")).thenReturn(requestBodySpec);
        when(requestBodySpec.body(bodyCaptor.capture())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(List.of());

        // When
        service.rerank("my query", List.of(first, second), 2);

        // Then
        HuggingFaceRerankerService.RerankRequest body = bodyCaptor.getValue();
        assertThat(body.query()).isEqualTo("my query");
        assertThat(body.texts()).containsExactly("first content", "second content");
        assertThat(body.truncate()).isTrue();
        assertThat(body.rawScores()).isFalse();
    }

    @Test
    void rerank_shouldReturnOriginalOrder_whenRestClientThrows() {
        // Given
        PartialResource first = candidate("first", 0.7);
        PartialResource second = candidate("second", 0.3);
        stubExchange();
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenThrow(new RestClientException("service down"));

        // When
        List<PartialResource> result = service.rerank("query", List.of(first, second), 2);

        // Then
        assertThat(result).containsExactly(first, second);
    }

    @Test
    void rerank_shouldReturnSortedCandidatesUnmodified_whenResultsIsEmpty() {
        // Given
        PartialResource first = candidate("first", 0.4);
        PartialResource second = candidate("second", 0.6);
        stubExchange();
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(List.of());

        // When
        List<PartialResource> result = service.rerank("query", List.of(first, second), 2);

        // Then
        assertThat(result).containsExactly(second, first);
    }

    private static PartialResource candidate(String content, double similarityScore) {
        PartialResource partialResource = new PartialResource();
        partialResource.setContent(content);
        partialResource.setSimilarityScore(similarityScore);
        return partialResource;
    }
}
