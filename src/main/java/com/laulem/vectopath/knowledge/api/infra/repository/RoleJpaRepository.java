package com.laulem.vectopath.knowledge.api.infra.repository;

import com.laulem.vectopath.knowledge.api.infra.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleJpaRepository extends JpaRepository<RoleEntity, Integer> {
    Optional<RoleEntity> findByRoleName(String roleName);
}
