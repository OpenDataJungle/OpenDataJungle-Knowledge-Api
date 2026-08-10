package com.opendatajungle.knowledge.api.infra.repository;

import com.opendatajungle.knowledge.api.business.model.ResourceStatus;
import com.opendatajungle.knowledge.api.infra.entity.ResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ResourceJpaRepository extends JpaRepository<ResourceEntity, UUID> {

    @Query(value = """
        SELECT r.*
        FROM knowledge.resource r
        LEFT JOIN knowledge.folder f ON r.folder_id = f.id
        WHERE (:id IS NULL OR r.id = CAST(:id AS uuid))
        AND (:status IS NULL OR r.status = :status)
        AND (:searchName IS NULL OR r.name LIKE :searchName)
        AND (:completePath IS NULL OR f.complete_path = :completePath)
        AND ( -- Security check: only return resources that the user has access to
            r.created_by = :username
            OR f.created_by = :username
            OR f.id IN (
                SELECT fg.folder_id FROM knowledge.folder_group fg
                INNER JOIN reference_data.group_users gu ON fg.group_id = gu.group_id
                INNER JOIN reference_data.users u ON gu.user_id = u.id AND u.username = :username
                INNER JOIN reference_data.permissions p ON gu.permission_id = p.id AND p.can_read = true
            )
            OR r.id IN (
                SELECT rgp.resource_id FROM knowledge.resource_group_permission rgp
                INNER JOIN reference_data.group_users gu ON rgp.group_id = gu.group_id
                INNER JOIN reference_data.users u ON gu.user_id = u.id AND u.username = :username
                INNER JOIN reference_data.permissions p ON rgp.permission_id = p.id AND p.can_read = true
            )
        )
        ORDER BY r.id, r.created_at DESC
        """, nativeQuery = true)
    List<ResourceEntity> findWithAccessControl(
            @Param("id") String id,
            @Param("status") String status,
            @Param("searchName") String searchName,
            @Param("completePath") String completePath,
            @Param("username") String username
    );

    @Modifying
    @Query("UPDATE ResourceEntity r SET r.status = :status WHERE r.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") ResourceStatus status);

    @Query(value = """
        SELECT EXISTS (
            SELECT 1 FROM knowledge.resource_group_permission rgp
            INNER JOIN reference_data.group_users gu ON rgp.group_id = gu.group_id
            INNER JOIN reference_data.users u ON gu.user_id = u.id AND u.username = :username
            INNER JOIN reference_data.permissions p ON rgp.permission_id = p.id AND p.can_write = true
            WHERE rgp.resource_id = :resourceId
        )
        """, nativeQuery = true)
    boolean hasGroupWriteAccess(@Param("resourceId") UUID resourceId, @Param("username") String username);
}
