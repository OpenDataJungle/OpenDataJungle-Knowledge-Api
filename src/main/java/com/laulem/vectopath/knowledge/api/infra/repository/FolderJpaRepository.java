package com.laulem.vectopath.knowledge.api.infra.repository;

import com.laulem.vectopath.knowledge.api.infra.entity.FolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FolderJpaRepository extends JpaRepository<FolderEntity, UUID> {
    boolean existsByCompletePath(String completePath);
    Optional<FolderEntity> findByCompletePath(String path);
}
