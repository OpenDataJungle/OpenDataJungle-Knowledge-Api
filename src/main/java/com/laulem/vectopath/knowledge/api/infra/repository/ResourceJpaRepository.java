package com.laulem.vectopath.knowledge.api.infra.repository;

import com.laulem.vectopath.knowledge.api.business.model.ResourceStatus;
import com.laulem.vectopath.knowledge.api.infra.entity.ResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ResourceJpaRepository extends JpaRepository<ResourceEntity, UUID> {

    @Query(value = """
            SELECT DISTINCT ON (r.id) r.*
            FROM knowledge.resources r
            LEFT JOIN knowledge.resource_allowed_roles rar ON r.id = rar.resource_id AND r.access_level = 'ROLE_LIST'
            LEFT JOIN knowledge.app_roles ar ON rar.role_id = ar.id
            WHERE (:id IS NULL OR r.id = CAST(:id AS uuid))
            AND (:status IS NULL OR r.status = :status)
            AND (:searchName IS NULL OR LOWER(r.name) LIKE LOWER(:searchName))
            AND (
                r.access_level = 'PUBLIC'
                OR (r.access_level = 'PRIVATE' AND r.created_by = :username)
                OR (r.access_level = 'ROLE_LIST' AND (ar.role_name = ANY(CAST(:userRoles AS text[])) OR r.created_by = :username))
            )
            ORDER BY r.id, r.created_at DESC
            """, nativeQuery = true)
    List<ResourceEntity> findWithAccessControl(
            @Param("id") String id,
            @Param("status") String status,
            @Param("searchName") String searchName,
            @Param("username") String username,
            @Param("userRoles") String[] userRoles
    );

    @Modifying
    @Query("UPDATE ResourceEntity r SET r.status = :status WHERE r.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") ResourceStatus status);
}
