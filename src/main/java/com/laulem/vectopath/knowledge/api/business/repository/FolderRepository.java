package com.laulem.vectopath.knowledge.api.business.repository;

import com.laulem.vectopath.knowledge.api.business.model.Folder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FolderRepository {

    Folder save(Folder folder);

    Optional<Folder> findById(UUID id);

    boolean existsById(UUID id);

    boolean existsByCompletePath(String completePath);

    List<Folder> findAll();

    void deleteById(UUID id);

    boolean hasCurrentUserWriteAccess(UUID folderId);

    boolean hasCurrentUserWriteGroupAccess(List<UUID> groupIds);

    Optional<UUID> findFolderIdByCompletePath(String completePath);

    List<UUID> getFolderGroupsIdByCompletePath(String completePath);
}
