package com.opendatajungle.knowledge.api.client.controller;

import com.opendatajungle.knowledge.api.business.model.PartialResource;
import com.opendatajungle.knowledge.api.business.service.impl.VectorizedResourceService;
import com.opendatajungle.knowledge.api.client.dto.SearchRequest;
import com.opendatajungle.knowledge.api.client.dto.SearchResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    @Mock
    private VectorizedResourceService vectorizedResourceService;

    @InjectMocks
    private SearchController controller;

    @Test
    void searchSemantic_shouldDelegateToService_andMapResults() {
        // Given
        List<UUID> resourceIds = List.of(UUID.randomUUID());
        SearchRequest request = new SearchRequest("what is opendatajungle", 5, 0.7, resourceIds);
        PartialResource partialResource = new PartialResource();
        partialResource.setResourceId(UUID.randomUUID());
        partialResource.setContent("opendatajungle is a search platform");
        when(vectorizedResourceService.searchSimilar("what is opendatajungle", 5, 0.7, resourceIds))
                .thenReturn(List.of(partialResource));

        // When
        List<SearchResponse> responses = controller.searchSemantic(request);

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().resourceId()).isEqualTo(partialResource.getResourceId());
        assertThat(responses.getFirst().content()).isEqualTo(partialResource.getContent());
        verify(vectorizedResourceService).searchSimilar("what is opendatajungle", 5, 0.7, resourceIds);
    }

    @Test
    void searchSemantic_shouldReturnEmptyList_whenNoMatches() {
        // Given
        SearchRequest request = new SearchRequest("no matches expected", 5, 0.7, null);
        when(vectorizedResourceService.searchSimilar("no matches expected", 5, 0.7, null))
                .thenReturn(List.of());

        // When
        List<SearchResponse> responses = controller.searchSemantic(request);

        // Then
        assertThat(responses).isEmpty();
    }
}
