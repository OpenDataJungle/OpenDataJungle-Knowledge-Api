package com.laulem.vectopath.knowledge.api.business.service.impl;

import com.laulem.vectopath.knowledge.api.business.exception.NotFoundException;
import com.laulem.vectopath.knowledge.api.business.exception.ParamException;
import com.laulem.vectopath.knowledge.api.business.model.Folder;
import com.laulem.vectopath.knowledge.api.business.repository.FolderRepository;
import com.laulem.vectopath.knowledge.api.business.service.FolderUseCase;
import com.laulem.vectopath.knowledge.api.shared.util.CollectionUtils;

import java.util.List;
import java.util.UUID;

public class FolderService implements FolderUseCase {
    private final FolderRepository folderRepository;

    public FolderService(FolderRepository folderRepository) {
        this.folderRepository = folderRepository;
    }

    @Override
    public Folder create(Folder folder) {
        validateFolderCreation(folder);
        setDefaultGroupIfNotSet(folder);
        return folderRepository.save(folder);
    }

    @Override
    public Folder getById(UUID id) {
        return folderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Folder", id.toString()));
    }

    @Override
    public List<Folder> listAll() {
        return folderRepository.findAll();
    }

    @Override
    public Folder update(Folder folder) {
        validateFolderUpdate(folder);
        Folder existing = folderRepository.findById(folder.getId()).orElseThrow(() -> new NotFoundException("Folder", folder.getId().toString()));

        setDefaultGroupIfNotSet(folder);

        String completePath = getCompletePath(folder);
        if (!existing.getCompletePath().equals(completePath) && folderRepository.existsByCompletePath(completePath)) {
            throw new ParamException("FOLDER_PATH_EXISTS", "A folder with path '" + completePath + "' already exists", "path");
        }

        existing.setName(folder.getName());
        existing.setPath(folder.getPath());

        return folderRepository.save(existing);
    }

    private void validateFolderCreation(final Folder folder) {
        if (folder.getPath() == null || folder.getName() == null) {
            throw new ParamException("FOLDER_PATH_OR_NAME_NULL", "Folder path and name cannot be null", "path");
        }
        if (CollectionUtils.isNotEmpty(folder.getGroupIds()) && !folderRepository.hasCurrentUserWriteGroupAccess(folder.getGroupIds())) {
            throw new ParamException("FOLDER_GROUP_ACCESS_DENIED", "Current user does not have write access to the specified group", "groupId");
        }
        // Check the user can access the parent folder with write access
        UUID parentFolderId = folderRepository.findFolderIdByCompletePath(folder.getPath())
                .orElseThrow(() -> new ParamException("FOLDER_PARENT_NOT_FOUND", "Parent folder with path '" + folder.getPath() + "' does not exist", "path"));

        if (!folderRepository.hasCurrentUserWriteAccess(parentFolderId)) {
            throw new ParamException("FOLDER_PARENT_ACCESS_DENIED", "Current user does not have write access to the parent folder", "path");
        }

        String completePath = getCompletePath(folder);
        if (folderRepository.existsByCompletePath(completePath)) {
            throw new ParamException("FOLDER_PATH_EXISTS", "A folder with path '" + completePath + "' already exists", "path");
        }
    }

    private void validateFolderUpdate(final Folder folder) {
        if (folder.getPath() == null || folder.getName() == null) {
            throw new ParamException("FOLDER_PATH_OR_NAME_NULL", "Folder path and name cannot be null", "path");
        }

        if (!folderRepository.hasCurrentUserWriteAccess(folder.getId())) {
            throw new ParamException("FOLDER_ACCESS_DENIED", "Current user does not have write access to the folder", "path");
        }

        if (CollectionUtils.isNotEmpty(folder.getGroupIds()) && !folderRepository.hasCurrentUserWriteGroupAccess(folder.getGroupIds())) {
            throw new ParamException("FOLDER_GROUP_ACCESS_DENIED", "Current user does not have write access to the specified group", "groupId");
        }
    }

    @Override
    public void delete(UUID id) {
        if (!folderRepository.existsById(id)) {
            throw new NotFoundException("Folder", id.toString());
        }
        folderRepository.deleteById(id);
    }

    private String getCompletePath(final Folder folder) {
        return folder.getPath() + "/" + folder.getName();
    }

    private void setDefaultGroupIfNotSet(final Folder folder) {
        if (CollectionUtils.isEmpty(folder.getGroupIds())) {
            folder.setGroupIds(folderRepository.getFolderGroupsIdByCompletePath(folder.getPath()));
        }
    }
}
