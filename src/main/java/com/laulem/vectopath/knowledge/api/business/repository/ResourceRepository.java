package com.laulem.vectopath.knowledge.api.business.repository;

import com.laulem.vectopath.knowledge.api.business.model.Resource;
import com.laulem.vectopath.knowledge.api.business.model.ResourceStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ARCHITECTURAL COMPROMISE:
 * Search methods include business logic (access control)
 * embedded directly in SQL queries for performance reasons.
 */
public interface ResourceRepository {

    Resource save(Resource resource);

    Optional<Resource> findById(UUID id);

    List<Resource> findAll();

    List<Resource> findByStatus(ResourceStatus status);

    List<Resource> findByNameContainingIgnoreCase(String name);

    void deleteById(UUID id);

    void updateStatus(Resource resource);

    void updateName(UUID id, String newName);
}
