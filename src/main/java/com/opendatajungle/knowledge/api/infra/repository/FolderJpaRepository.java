package com.opendatajungle.knowledge.api.infra.repository;

import com.opendatajungle.knowledge.api.infra.entity.FolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FolderJpaRepository extends JpaRepository<FolderEntity, UUID> {
    boolean existsByCompletePath(String completePath);
    Optional<FolderEntity> findByCompletePath(String path);

    @Query(value = """
        SELECT f.*
        FROM knowledge.folder f
        WHERE ( -- Security check: only return folders that the user has access to
            f.created_by = :username
            OR f.id IN (
                SELECT fg.folder_id FROM knowledge.folder_group fg
                INNER JOIN referential.group_users gu ON fg.group_id = gu.group_id
                INNER JOIN referential.users u ON gu.user_id = u.id AND u.username = :username
                INNER JOIN referential.permissions p ON gu.permission_id = p.id AND p.can_read = true
            )
        )
        ORDER BY f.id
        """, nativeQuery = true)
    List<FolderEntity> findAllWithAccessControl(@Param("username") String username);

    @Query(value = """
        SELECT f.*
        FROM knowledge.folder f
        WHERE f.id = :folderId
        AND ( -- Security check: only return the folder if the user has read access to it
            f.created_by = :username
            OR f.id IN (
                SELECT fg.folder_id FROM knowledge.folder_group fg
                INNER JOIN referential.group_users gu ON fg.group_id = gu.group_id
                INNER JOIN referential.users u ON gu.user_id = u.id AND u.username = :username
                INNER JOIN referential.permissions p ON gu.permission_id = p.id AND p.can_read = true
            )
        )
        """, nativeQuery = true)
    Optional<FolderEntity> findByIdWithAccessControl(@Param("folderId") UUID folderId, @Param("username") String username);

    @Query(value = """
        SELECT d.*
        FROM knowledge.folder d
        WHERE d.parent_id = :folderId
        AND ( -- Security check: only return folders that the user has access to
            d.created_by = :username
            OR d.id IN (
                SELECT fg.folder_id FROM knowledge.folder_group fg
                INNER JOIN referential.group_users gu ON fg.group_id = gu.group_id
                INNER JOIN referential.users u ON gu.user_id = u.id AND u.username = :username
                INNER JOIN referential.permissions p ON gu.permission_id = p.id AND p.can_read = true
            )
        )
        ORDER BY d.id
        """, nativeQuery = true)
    List<FolderEntity> findAllChildrenWithAccessControl(@Param("folderId") UUID folderId, @Param("username") String username);
}
