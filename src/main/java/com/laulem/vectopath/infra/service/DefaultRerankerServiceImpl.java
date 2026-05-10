package com.laulem.vectopath.infra.service;

import com.laulem.vectopath.business.model.PartialResource;
import com.laulem.vectopath.business.service.RerankerService;

import java.util.Comparator;
import java.util.List;

public class DefaultRerankerServiceImpl implements RerankerService {
    @Override
    public List<PartialResource> rerank(String query, List<PartialResource> candidates, int limit) {
        return candidates.stream()
                .sorted(Comparator.comparingDouble(PartialResource::getSimilarityScore).reversed())
                .limit(limit)
                .toList();
    }
}
