package com.laulem.vectopath.knowledge.api.infra.service;

import com.laulem.vectopath.knowledge.api.business.model.PartialResource;
import com.laulem.vectopath.knowledge.api.business.service.RerankerUseCase;

import java.util.Comparator;
import java.util.List;

public class DefaultRerankerService implements RerankerUseCase {
    @Override
    public List<PartialResource> rerank(String query, List<PartialResource> candidates, int limit) {
        return candidates.stream()
                .sorted(Comparator.comparingDouble(PartialResource::getSimilarityScore).reversed())
                .limit(limit)
                .toList();
    }
}
