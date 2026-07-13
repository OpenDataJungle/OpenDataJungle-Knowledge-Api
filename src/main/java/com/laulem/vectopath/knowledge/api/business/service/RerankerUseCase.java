package com.laulem.vectopath.knowledge.api.business.service;

import com.laulem.vectopath.knowledge.api.business.model.PartialResource;

import java.util.List;

public interface RerankerUseCase {
    List<PartialResource> rerank(String query, List<PartialResource> candidates, int limit);
}
