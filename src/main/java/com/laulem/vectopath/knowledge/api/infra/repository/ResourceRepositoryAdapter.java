package com.laulem.vectopath.knowledge.api.infra.repository;

import com.laulem.vectopath.knowledge.api.business.exception.NotFoundException;
import com.laulem.vectopath.knowledge.api.business.model.Resource;
import com.laulem.vectopath.knowledge.api.business.model.ResourceStatus;
import com.laulem.vectopath.knowledge.api.business.repository.ResourceRepository;
import com.laulem.vectopath.knowledge.api.business.service.AuthenticationUseCase;
import com.laulem.vectopath.knowledge.api.infra.entity.ResourceEntity;
import com.laulem.vectopath.knowledge.api.infra.entity.RoleEntity;
import com.laulem.vectopath.knowledge.api.shared.util.DateUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
public class ResourceRepositoryAdapter implements ResourceRepository {
    private final ResourceJpaRepository jpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final AuthenticationUseCase authenticationUseCase;

    public ResourceRepositoryAdapter(ResourceJpaRepository jpaRepository,
                                     RoleJpaRepository roleJpaRepository,
                                     AuthenticationUseCase authenticationUseCase) {
        this.jpaRepository = jpaRepository;
        this.roleJpaRepository = roleJpaRepository;
        this.authenticationUseCase = authenticationUseCase;
    }

    @Override
    @Transactional
    public Resource save(Resource resource) {
        ResourceEntity entity = ResourceEntity.fromDomain(resource);
        entity.setAllowedRoles(getRoleEntities(resource));

        ResourceEntity savedEntity = jpaRepository.save(entity);

        return savedEntity.toDomain();
    }

    private List<RoleEntity> getRoleEntities(Resource resource) {
        return Stream.ofNullable(resource.getAllowedRoles())
                .flatMap(List::stream)
                .map(roleJpaRepository::findByRoleName)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Resource> findById(UUID id) {
        String username = authenticationUseCase.getCurrentUser();
        List<String> userRoles = authenticationUseCase.getAuthorities();

        List<ResourceEntity> results = jpaRepository.findWithAccessControl(
            id.toString(),
            null,
            null,
            username,
            userRoles.toArray(new String[0])
        );

        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst().toDomain());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Resource> findAll() {
        String username = authenticationUseCase.getCurrentUser();
        List<String> userRoles = authenticationUseCase.getAuthorities();

        return jpaRepository.findWithAccessControl(
            null,
            null,
            null,
            username,
            userRoles.toArray(new String[0])
        ).stream()
        .map(ResourceEntity::toDomain)
        .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Resource> findByStatus(ResourceStatus status) {
        String username = authenticationUseCase.getCurrentUser();
        List<String> userRoles = authenticationUseCase.getAuthorities();

        return jpaRepository.findWithAccessControl(
            null,
            status.name(),
            null,
            username,
            userRoles.toArray(new String[0])
        ).stream()
        .map(ResourceEntity::toDomain)
        .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Resource> findByNameContainingIgnoreCase(String name) {
        String username = authenticationUseCase.getCurrentUser();
        List<String> userRoles = authenticationUseCase.getAuthorities();

        return jpaRepository.findWithAccessControl(
            null,
            null,
            name,
            username,
            userRoles.toArray(new String[0])
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
}
