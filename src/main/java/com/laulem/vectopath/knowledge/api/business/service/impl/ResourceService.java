package com.laulem.vectopath.knowledge.api.business.service.impl;

import com.laulem.vectopath.knowledge.api.business.exception.NotFoundException;
import com.laulem.vectopath.knowledge.api.business.exception.ParamException;
import com.laulem.vectopath.knowledge.api.business.exception.VectorizationException;
import com.laulem.vectopath.knowledge.api.business.model.Resource;
import com.laulem.vectopath.knowledge.api.business.model.ResourceGroupPermission;
import com.laulem.vectopath.knowledge.api.business.model.ResourceStatus;
import com.laulem.vectopath.knowledge.api.business.repository.ResourceRepository;
import com.laulem.vectopath.knowledge.api.business.repository.VectorStoreRepository;
import com.laulem.vectopath.knowledge.api.business.service.AuthenticationUseCase;
import com.laulem.vectopath.knowledge.api.business.service.FolderUseCase;
import com.laulem.vectopath.knowledge.api.business.service.ReferentialUseCase;
import com.laulem.vectopath.knowledge.api.business.service.ResourceUseCase;
import com.laulem.vectopath.knowledge.api.shared.util.CollectionUtils;
import com.laulem.vectopath.knowledge.api.shared.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ResourceService implements ResourceUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ResourceService.class);

    private final ResourceRepository resourceRepository;
    private final VectorStoreRepository vectorRepository;
    private final FolderUseCase folderUseCase;
    private final AuthenticationUseCase authenticationUseCase;
    private final ReferentialUseCase referentialUseCase;

    public ResourceService(ResourceRepository resourceRepository,
                           VectorStoreRepository vectorRepository,
                           final FolderUseCase folderUseCase,
                           final AuthenticationUseCase authenticationUseCase,
                           final ReferentialUseCase referentialUseCase) {
        this.resourceRepository = resourceRepository;
        this.vectorRepository = vectorRepository;
        this.folderUseCase = folderUseCase;
        this.authenticationUseCase = authenticationUseCase;
        this.referentialUseCase = referentialUseCase;
    }

    @Override
    public Resource createResource(Resource resource) {
        if (resource.getFolderId() != null && !folderUseCase.hasCurrentUserWriteAccess(resource.getFolderId())) {
            throw new NotFoundException("Folder", resource.getFolderId().toString());
        } else if (resource.getFolderId() == null) {
            resource.setFolderId(folderUseCase.getOrCreateDefaultFolder().getId());
        }

        validateGroupPermissions(resource.getGroupPermissions());

        return processResourceVectorization(resource);
    }

    private void validateGroupPermissions(List<ResourceGroupPermission> groupPermissions) {
        if (CollectionUtils.isEmpty(groupPermissions)) {
            return;
        }

        List<UUID> groupIds = groupPermissions.stream().map(ResourceGroupPermission::getGroupId).toList();
        if (!referentialUseCase.hasCurrentUserWriteGroupAccess(groupIds)) {
            throw new ParamException("RESOURCE_GROUP_ACCESS_DENIED", "Current user does not have write access to the specified group", "groupId");
        }
    }

    @Override
    public Optional<Resource> findById(UUID id) {
        return resourceRepository.findByIdWithAccessControl(id);
    }

    @Override
    public List<Resource> findAll() {
        // Security is handled at the infrastructure layer for performance reasons.
        return resourceRepository.findAllWithAccessControl();
    }

    @Override
    public List<Resource> findByStatus(ResourceStatus status) {
        // Security is handled at the infrastructure layer for performance reasons.
        return resourceRepository.findByStatusWithAccessControl(status);
    }

    @Override
    public List<Resource> searchResources(String name, String path) {
        if (!StringUtils.hasText(name) && !StringUtils.hasText(path)) {
            throw new ParamException("REQUIRED", "At least one of 'name' or 'path' must be provided", "");
        }

        // Security is handled at the infrastructure layer for performance reasons.
        return resourceRepository.searchWithAccessControl(name, path);
    }

    @Override
    public void deleteResource(UUID id) {
        getWritableResource(id);

        logger.info("Deleting resource: {}", id);

        vectorRepository.deleteResource(id);
        resourceRepository.deleteById(id);
    }

    @Override
    public Resource reprocessResource(UUID id) {
        Resource resource = getWritableResource(id);

        logger.info("Reprocessing resource: {}", StringUtils.sanitizeForLog(resource.getName()));

        vectorRepository.deleteResource(id);
        return processResourceVectorization(resource);
    }

    @Override
    public void renameResource(UUID id, String newName) {
        getWritableResource(id);
        resourceRepository.updateName(id, newName);
    }

    private Resource getWritableResource(UUID id) {
        Resource resource = resourceRepository.findByIdWithAccessControl(id)
                .orElseThrow(() -> new NotFoundException("Resource", id.toString()));

        if (!hasCurrentUserWriteAccess(resource)) {
            throw new NotFoundException("Resource", id.toString());
        }
        return resource;
    }

    private boolean hasCurrentUserWriteAccess(Resource resource) {
        if (authenticationUseCase.getCurrentUser().equals(resource.getCreatedBy())) {
            return true;
        }

        return resource.getFolderId() != null
                && (folderUseCase.hasCurrentUserWriteAccess(resource.getFolderId()) || resourceRepository.hasCurrentUserWriteAccess(resource.getId()));
    }

    private Resource processResourceVectorization(Resource resource) {
        try {
            resource.setStatus(ResourceStatus.PROCESSING);
            resource = resourceRepository.save(resource);

            vectorRepository.addResource(resource);

            resource.setStatus(ResourceStatus.VECTORIZED);
            resourceRepository.updateStatus(resource);

            logger.info("Vectorization completed successfully for resource: {}", StringUtils.sanitizeForLog(resource.getName()));
            return resource;
        } catch (Exception e) {
            resource.setStatus(ResourceStatus.ERROR);
            resourceRepository.updateStatus(resource);

            throw new VectorizationException(resource.getName(), e);
        }
    }
}
