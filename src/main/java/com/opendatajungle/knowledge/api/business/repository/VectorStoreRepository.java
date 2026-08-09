package com.opendatajungle.knowledge.api.business.repository;

import com.opendatajungle.knowledge.api.business.model.PartialResource;
import com.opendatajungle.knowledge.api.business.model.Resource;

import java.util.List;
import java.util.UUID;

public interface VectorStoreRepository {

    void addResource(Resource resource);

    List<PartialResource> searchSimilar(String query, int limit, double minSimilarity, String currentUser, List<UUID> resourceIds);

    void deleteResource(UUID resourceId);
}
