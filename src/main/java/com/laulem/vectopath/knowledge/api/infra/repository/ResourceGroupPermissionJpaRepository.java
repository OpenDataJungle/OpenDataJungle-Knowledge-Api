package com.laulem.vectopath.knowledge.api.infra.repository;

import com.laulem.vectopath.knowledge.api.infra.entity.ResourceGroupPermissionEntity;
import com.laulem.vectopath.knowledge.api.infra.entity.ResourceGroupPermissionIdEmbeddable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceGroupPermissionJpaRepository extends JpaRepository<ResourceGroupPermissionEntity, ResourceGroupPermissionIdEmbeddable> {
}
