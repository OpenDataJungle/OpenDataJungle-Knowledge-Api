package com.opendatajungle.knowledge.api.business.service.impl;

import com.opendatajungle.knowledge.api.business.exception.ParamException;
import com.opendatajungle.knowledge.api.business.model.PartialResource;
import com.opendatajungle.knowledge.api.business.repository.VectorStoreRepository;
import com.opendatajungle.knowledge.api.business.service.AuthenticationUseCase;
import com.opendatajungle.knowledge.api.business.service.RerankerUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VectorizedResourceServiceTest {

    @Mock
    private VectorStoreRepository vectorRepository;

    @Mock
    private AuthenticationUseCase authenticationUseCase;

    @Mock
    private RerankerUseCase rerankerUseCase;

    @InjectMocks
    private VectorizedResourceService service;

    @Test
    void searchSimilar_shouldThrowParamException_whenQueryIsBlank() {
        // When & Then
        assertThatThrownBy(() -> service.searchSimilar(" ", 5, 0.5, null))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getCode()).isEqualTo("REQUIRED"));
    }

    @Test
    void searchSimilar_shouldFetchCandidatePoolAndRerankDownToLimit() {
        // Given
        List<UUID> resourceIds = List.of(UUID.randomUUID());
        PartialResource candidate1 = new PartialResource();
        PartialResource candidate2 = new PartialResource();
        List<PartialResource> candidates = List.of(candidate1, candidate2);
        List<PartialResource> reranked = List.of(candidate1);

        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");
        when(vectorRepository.searchSimilar("opendatajungle", 15, 0.5, "alice", resourceIds)).thenReturn(candidates);
        when(rerankerUseCase.rerank("opendatajungle", candidates, 5)).thenReturn(reranked);

        // When
        List<PartialResource> result = service.searchSimilar("opendatajungle", 5, 0.5, resourceIds);

        // Then
        assertThat(result).isEqualTo(reranked);
        verify(vectorRepository).searchSimilar("opendatajungle", 15, 0.5, "alice", resourceIds);
        verify(rerankerUseCase).rerank("opendatajungle", candidates, 5);
    }
}
