package com.laulem.vectopath.knowledge.api.business.service.impl;

import com.laulem.vectopath.knowledge.api.business.model.PartialResource;
import com.laulem.vectopath.knowledge.api.business.repository.VectorStoreRepository;
import com.laulem.vectopath.knowledge.api.business.service.AuthenticationUseCase;
import com.laulem.vectopath.knowledge.api.business.service.RerankerUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public class VectorizedResourceService {

    public static final int RERANK_CANDIDATE_MULTIPLIER = 3;
    private static final Logger logger = LoggerFactory.getLogger(VectorizedResourceService.class);
    private final VectorStoreRepository vectorRepository;
    private final AuthenticationUseCase authenticationUseCase;
    private final RerankerUseCase rerankerUseCase;

    public VectorizedResourceService(VectorStoreRepository vectorRepository,
                                     AuthenticationUseCase authenticationUseCase,
                                     RerankerUseCase rerankerUseCase) {
        this.vectorRepository = vectorRepository;
        this.authenticationUseCase = authenticationUseCase;
        this.rerankerUseCase = rerankerUseCase;
    }

    public List<PartialResource> searchSimilar(String query, int limit, double minSimilarity, List<UUID> resourceIds) {
        logger.info("Semantic search for: {}", query);

        String currentUser = authenticationUseCase.getCurrentUser();

        int candidatePoolSize = limit * RERANK_CANDIDATE_MULTIPLIER;
        List<PartialResource> candidates = vectorRepository.searchSimilar(query, candidatePoolSize, minSimilarity, currentUser, resourceIds);

        logger.debug("Re-ranking {} candidates down to {} results", candidates.size(), limit);
        return rerankerUseCase.rerank(query, candidates, limit);
    }
}

