package com.laulem.vectopath.business.service.impl;

import com.laulem.vectopath.business.exception.ResourceDeletionException;
import com.laulem.vectopath.business.model.PartialResource;
import com.laulem.vectopath.business.repository.VectorStoreRepository;
import com.laulem.vectopath.business.service.AuthenticationService;
import com.laulem.vectopath.business.service.RerankerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public class VectorizedResourceService {

    private static final Logger logger = LoggerFactory.getLogger(VectorizedResourceService.class);

    private final VectorStoreRepository vectorRepository;
    private final AuthenticationService authenticationService;
    private final RerankerService rerankerService;

    public VectorizedResourceService(VectorStoreRepository vectorRepository,
                                     AuthenticationService authenticationService,
                                     RerankerService rerankerService) {
        this.vectorRepository = vectorRepository;
        this.authenticationService = authenticationService;
        this.rerankerService = rerankerService;
    }

    public List<PartialResource> searchSimilar(String query, int limit, double minSimilarity, List<UUID> resourceIds) {
        logger.info("Semantic search for: {}", query);

        String currentUser = authenticationService.getCurrentUser();
        List<String> userAuthorities = authenticationService.getAuthorities();

        int candidatePoolSize = limit * 3;
        List<PartialResource> candidates = vectorRepository.searchSimilar(query, candidatePoolSize, minSimilarity, currentUser, userAuthorities, resourceIds);

        logger.debug("Re-ranking {} candidates down to {} results", candidates.size(), limit);
        return rerankerService.rerank(query, candidates, limit);
    }

    public void deleteResource(UUID resourceId) {
        logger.info("Deleting resource: {}", resourceId);
        try {
            vectorRepository.deleteResource(resourceId);
        } catch (Exception e) {
            throw new ResourceDeletionException(resourceId, e);
        }
    }
}

