package com.laulem.vectopath.knowledge.api.business.service;

import com.laulem.vectopath.knowledge.api.business.model.Folder;

import java.util.List;
import java.util.UUID;

public interface FolderUseCase {
    Folder create(Folder folder);

    Folder getById(UUID folderId);

    List<Folder> listAll();

    List<Folder> findAllChildren(UUID folderId);

    Folder update(Folder folder);

    void delete(UUID folderId);

    Folder getOrCreateDefaultFolder();

    boolean hasCurrentUserWriteAccess(UUID folderId);
}
