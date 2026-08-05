package com.laulem.vectopath.knowledge.api.infra.repository;

import com.laulem.vectopath.knowledge.api.business.exception.NotFoundException;
import com.laulem.vectopath.knowledge.api.business.model.Resource;
import com.laulem.vectopath.knowledge.api.business.model.ResourceGroupPermission;
import com.laulem.vectopath.knowledge.api.business.model.ResourceStatus;
import com.laulem.vectopath.knowledge.api.business.repository.ResourceRepository;
import com.laulem.vectopath.knowledge.api.business.service.AuthenticationUseCase;
import com.laulem.vectopath.knowledge.api.infra.entity.ResourceEntity;
import com.laulem.vectopath.knowledge.api.infra.entity.ResourceGroupPermissionEntity;
import com.laulem.vectopath.knowledge.api.infra.entity.ResourceGroupPermissionIdEmbeddable;
import com.laulem.vectopath.knowledge.api.shared.util.CollectionUtils;
import com.laulem.vectopath.knowledge.api.shared.util.DateUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ResourceRepositoryAdapter implements ResourceRepository {
    private final ResourceJpaRepository jpaRepository;
    private final ResourceGroupPermissionJpaRepository groupPermissionJpaRepository;
    private final AuthenticationUseCase authenticationUseCase;

    public ResourceRepositoryAdapter(ResourceJpaRepository jpaRepository,
                                     ResourceGroupPermissionJpaRepository groupPermissionJpaRepository,
                                     AuthenticationUseCase authenticationUseCase) {
        this.jpaRepository = jpaRepository;
        this.groupPermissionJpaRepository = groupPermissionJpaRepository;
        this.authenticationUseCase = authenticationUseCase;
    }

    @Override
    @Transactional
    public Resource save(Resource resource) {
        ResourceEntity entity = ResourceEntity.fromDomain(resource);

        ResourceEntity savedEntity = jpaRepository.save(entity);

        if (CollectionUtils.isNotEmpty(resource.getGroupPermissions())) {
            assignGroupPermissions(savedEntity.getId(), resource.getGroupPermissions());
        }

        return savedEntity.toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Resource> findByIdWithAccessControl(UUID id) {
        String username = authenticationUseCase.getCurrentUser();

        List<ResourceEntity> results = jpaRepository.findWithAccessControl(
                id.toString(),
                null,
                null,
                null,
                username
        );

        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst().toDomain());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Resource> findAllWithAccessControl() {
        String username = authenticationUseCase.getCurrentUser();

        return jpaRepository.findWithAccessControl(
                        null,
                        null,
                        null,
                        null,
                        username
                ).stream()
                .map(ResourceEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Resource> findByStatusWithAccessControl(ResourceStatus status) {
        String username = authenticationUseCase.getCurrentUser();

        return jpaRepository.findWithAccessControl(
                        null,
                        status.name(),
                        null,
                        null,
                        username
                ).stream()
                .map(ResourceEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Resource> searchWithAccessControl(String name, String path) {
        String username = authenticationUseCase.getCurrentUser();

        return jpaRepository.findWithAccessControl(
                        null,
                        null,
                        name,
                        path,
                        username
                ).stream()
                .map(ResourceEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void updateStatus(Resource resource) {
        jpaRepository.updateStatus(resource.getId(), resource.getStatus());
    }

    @Override
    @Transactional
    public void updateName(UUID id, String newName) {
        ResourceEntity resource = jpaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Resource", id.toString()));

        resource.setUpdatedAt(DateUtils.now());
        resource.setName(newName);
        jpaRepository.save(resource);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasCurrentUserWriteAccess(UUID resourceId) {
        ResourceEntity resource = jpaRepository.findById(resourceId)
                .orElseThrow(() -> new NotFoundException("Resource", resourceId.toString()));

        String currentUser = authenticationUseCase.getCurrentUser();
        if (currentUser.equals(resource.getCreatedBy())) {
            return true;
        }

        return jpaRepository.hasGroupWriteAccess(resourceId, currentUser);
    }

    private void assignGroupPermissions(final UUID resourceId, final List<ResourceGroupPermission> groupPermissions) {
        List<ResourceGroupPermissionEntity> entities = groupPermissions.stream()
                .map(groupPermission -> ResourceGroupPermissionEntity.builder()
                        .id(new ResourceGroupPermissionIdEmbeddable(resourceId, groupPermission.getGroupId()))
                        .permissionId(groupPermission.getPermissionId())
                        .build())
                .toList();
        groupPermissionJpaRepository.saveAll(entities);
    }
}
