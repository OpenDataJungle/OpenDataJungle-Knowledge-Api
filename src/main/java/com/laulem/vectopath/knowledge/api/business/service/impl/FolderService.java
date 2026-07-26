package com.laulem.vectopath.knowledge.api.business.service.impl;

import com.laulem.vectopath.knowledge.api.business.exception.NotFoundException;
import com.laulem.vectopath.knowledge.api.business.exception.ParamException;
import com.laulem.vectopath.knowledge.api.business.model.Folder;
import com.laulem.vectopath.knowledge.api.business.repository.FolderRepository;
import com.laulem.vectopath.knowledge.api.business.service.AuthenticationUseCase;
import com.laulem.vectopath.knowledge.api.business.service.FolderUseCase;
import com.laulem.vectopath.knowledge.api.shared.util.CollectionUtils;

import java.util.List;
import java.util.UUID;

public class FolderService implements FolderUseCase {
    private static final String ROOT_PATH = "ROOT";
    private static final String USERS_FOLDER_NAME = "USERS";
    public static final String FOLDER_DELIMITER = "/";

    private final FolderRepository folderRepository;
    private final AuthenticationUseCase authenticationUseCase;

    public FolderService(FolderRepository folderRepository, AuthenticationUseCase authenticationUseCase) {
        this.folderRepository = folderRepository;
        this.authenticationUseCase = authenticationUseCase;
    }

    @Override
    public Folder create(Folder folder) {
        validateFolderCreation(folder);
        setDefaultGroupIfNotSet(folder);
        return folderRepository.save(folder);
    }

    @Override
    public Folder getById(UUID folderId) {
        return folderRepository.findByIdWithAccessControl(folderId)
                .orElseThrow(() -> new NotFoundException("Folder", folderId.toString()));
    }

    @Override
    public List<Folder> listAll() {
        return folderRepository.findAllWithAccessControl();
    }

    @Override
    public List<Folder> findAllChildren(UUID folderId) {
        if (folderRepository.findByIdWithAccessControl(folderId).isEmpty()) {
            throw new NotFoundException("Folder", folderId.toString());
        }
        return folderRepository.findAllChildrenWithAccessControl(folderId);
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

        folder.setParentId(parentFolderId);

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
    public void delete(UUID folderId) {
        if (!folderRepository.hasCurrentUserWriteAccess(folderId)) {
            throw new NotFoundException("Folder", folderId.toString());
        }
        folderRepository.deleteById(folderId);
    }

    @Override
    public Folder getOrCreateDefaultFolder() {
        String parentPath = ROOT_PATH + FOLDER_DELIMITER + USERS_FOLDER_NAME;
        return getOrCreateFolder(parentPath, authenticationUseCase.getCurrentUser());
    }

    @Override
    public boolean hasCurrentUserWriteAccess(UUID folderId) {
        return folderRepository.hasCurrentUserWriteAccess(folderId);
    }

    private Folder getOrCreateFolder(String parentPath, String username) {
        String completePath = parentPath + FOLDER_DELIMITER + username;
        return folderRepository.findByCompletePath(completePath)
                .orElseGet(() -> folderRepository.save(new Folder(username, parentPath, null, username)));
    }

    private String getCompletePath(final Folder folder) {
        return folder.getPath() + FOLDER_DELIMITER + folder.getName();
    }

    private void setDefaultGroupIfNotSet(final Folder folder) {
        if (CollectionUtils.isEmpty(folder.getGroupIds())) {
            folder.setGroupIds(folderRepository.getFolderGroupsIdByCompletePath(folder.getPath()));
        }
    }
}
