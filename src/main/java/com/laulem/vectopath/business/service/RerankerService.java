package com.laulem.vectopath.business.service;

import com.laulem.vectopath.business.model.PartialResource;

import java.util.List;

public interface RerankerService {
    List<PartialResource> rerank(String query, List<PartialResource> candidates, int limit);
}
