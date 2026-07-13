package com.laulem.vectopath.knowledge.api.business.service.impl;

import com.laulem.vectopath.knowledge.api.business.exception.NotFoundException;
import com.laulem.vectopath.knowledge.api.business.exception.VectorizationException;
import com.laulem.vectopath.knowledge.api.business.model.Resource;
import com.laulem.vectopath.knowledge.api.business.model.ResourceStatus;
import com.laulem.vectopath.knowledge.api.business.repository.ResourceRepository;
import com.laulem.vectopath.knowledge.api.business.repository.VectorStoreRepository;
import com.laulem.vectopath.knowledge.api.business.service.ResourceUseCase;
import com.laulem.vectopath.knowledge.api.business.service.RoleValidationUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ResourceService implements ResourceUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ResourceService.class);

    private final ResourceRepository resourceRepository;
    private final VectorizedResourceService vectorizedResourceService;
    private final VectorStoreRepository vectorRepository;
    private final RoleValidationUseCase roleValidationUseCase;

    public ResourceService(ResourceRepository resourceRepository,
                           VectorizedResourceService vectorizedResourceService,
                           VectorStoreRepository vectorRepository, final RoleValidationUseCase roleValidationUseCase) {
        this.resourceRepository = resourceRepository;
        this.vectorizedResourceService = vectorizedResourceService;
        this.vectorRepository = vectorRepository;
        this.roleValidationUseCase = roleValidationUseCase;
    }

    @Override
    public Resource createResource(Resource resource) {
        if (resource.getAccessLevel() == null) {
            resource.setAccessLevel(Resource.AccessLevel.PRIVATE);
        }
        roleValidationUseCase.validateAllowedRoles(resource.getAllowedRoles());

        resource = processResourceVectorization(resource);
        return resource;
    }

    @Override
    public Optional<Resource> getResourceById(UUID id) {
        return resourceRepository.findById(id);
    }

    @Override
    public List<Resource> getAllResources() {
        return resourceRepository.findAll();
    }

    @Override
    public List<Resource> getResourcesByStatus(ResourceStatus status) {
        return resourceRepository.findByStatus(status);
    }

    @Override
    public List<Resource> searchResourcesByName(String name) {
        return resourceRepository.findByNameContainingIgnoreCase(name);
    }

    @Override
    public void deleteResource(UUID id) {
        logger.info("Deleting resource: {}", id);

        vectorizedResourceService.deleteResource(id);
        resourceRepository.deleteById(id);
    }

    @Override
    public Resource reprocessResource(UUID id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Resource", id.toString()));

        logger.info("Reprocessing resource: {}", resource.getName());

        vectorizedResourceService.deleteResource(id);
        resource = processResourceVectorization(resource);

        return resource;
    }

    @Override
    public void renameResource(UUID id, String newName) {
        resourceRepository.updateName(id, newName);
    }

    private Resource processResourceVectorization(Resource resource) {
        try {
            resource.setStatus(ResourceStatus.PROCESSING);
            resource = resourceRepository.save(resource);

            vectorRepository.addResource(resource);

            resource.setStatus(ResourceStatus.VECTORIZED);
            resourceRepository.updateStatus(resource);

            logger.info("Vectorization completed successfully for resource: {}", resource.getName());
            return resource;
        } catch (Exception e) {
            resource.setStatus(ResourceStatus.ERROR);
            resourceRepository.updateStatus(resource);

            throw new VectorizationException(resource.getName(), e);
        }
    }
}
