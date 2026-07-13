package com.laulem.vectopath.knowledge.api.business.service;

import com.laulem.vectopath.knowledge.api.business.model.Resource;
import com.laulem.vectopath.knowledge.api.business.model.ResourceStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResourceUseCase {

    Resource createResource(Resource resource);

    Optional<Resource> getResourceById(UUID id);

    List<Resource> getAllResources();

    List<Resource> getResourcesByStatus(ResourceStatus status);

    List<Resource> searchResourcesByName(String name);

    void deleteResource(UUID id);

    Resource reprocessResource(UUID id);

    void renameResource(UUID id, String newName);
}
