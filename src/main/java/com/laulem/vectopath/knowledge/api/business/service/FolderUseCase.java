package com.laulem.vectopath.knowledge.api.business.service;

import com.laulem.vectopath.knowledge.api.business.model.Folder;

import java.util.List;
import java.util.UUID;

public interface FolderUseCase {
    Folder create(Folder folder);

    Folder getById(UUID id);

    List<Folder> listAll();

    Folder update(Folder folder);

    void delete(UUID id);
}
