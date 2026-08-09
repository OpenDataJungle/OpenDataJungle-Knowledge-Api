package com.opendatajungle.knowledge.api.business.repository;

import com.opendatajungle.knowledge.api.business.model.Folder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FolderRepository {

    Folder save(Folder folder);

    Optional<Folder> findById(UUID id);

    Optional<Folder> findByCompletePath(String completePath);

    Optional<Folder> findByIdWithAccessControl(UUID id);

    boolean existsById(UUID id);

    boolean existsByCompletePath(String completePath);

    List<Folder> findAllWithAccessControl();

    List<Folder> findAllChildrenWithAccessControl(UUID folderId);

    void deleteById(UUID id);

    boolean hasCurrentUserWriteAccess(UUID folderId);

    Optional<UUID> findFolderIdByCompletePath(String completePath);

    List<UUID> getFolderGroupsIdByCompletePath(String completePath);
}
