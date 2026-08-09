package com.opendatajungle.knowledge.api.infra.repository;

import com.opendatajungle.knowledge.api.infra.entity.ResourceGroupPermissionEntity;
import com.opendatajungle.knowledge.api.infra.entity.ResourceGroupPermissionIdEmbeddable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceGroupPermissionJpaRepository extends JpaRepository<ResourceGroupPermissionEntity, ResourceGroupPermissionIdEmbeddable> {
}
