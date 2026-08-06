package com.laulem.vectopath.knowledge.api.business.service;

import com.laulem.vectopath.knowledge.api.business.model.Resource;
import com.laulem.vectopath.knowledge.api.business.model.ResourceStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResourceUseCase {

    Resource createResource(Resource resource);

    Optional<Resource> findById(UUID id);

    List<Resource> findAll();

    List<Resource> findByStatus(ResourceStatus status);

    List<Resource> searchResources(String name, String path);

    void deleteResource(UUID id);

    Resource reprocessResource(UUID id);

    void renameResource(UUID id, String newName);
}
