package com.laulem.vectopath.knowledge.api.business.service.impl;

import com.laulem.vectopath.knowledge.api.business.exception.NotFoundException;
import com.laulem.vectopath.knowledge.api.business.exception.ParamException;
import com.laulem.vectopath.knowledge.api.business.model.Folder;
import com.laulem.vectopath.knowledge.api.business.repository.FolderRepository;
import com.laulem.vectopath.knowledge.api.business.service.AuthenticationUseCase;
import com.laulem.vectopath.knowledge.api.business.service.FolderUseCase;
import com.laulem.vectopath.knowledge.api.business.service.ReferentialUseCase;
import com.laulem.vectopath.knowledge.api.shared.util.CollectionUtils;

import java.util.List;
import java.util.UUID;

public class FolderService implements FolderUseCase {
    private static final String ROOT_PATH = "ROOT";
    private static final String USERS_FOLDER_NAME = "USERS";
    public static final String FOLDER_DELIMITER = "/";

    private final FolderRepository folderRepository;
    private final AuthenticationUseCase authenticationUseCase;
    private final ReferentialUseCase referentialUseCase;

    public FolderService(FolderRepository folderRepository, AuthenticationUseCase authenticationUseCase, ReferentialUseCase referentialUseCase) {
        this.folderRepository = folderRepository;
        this.authenticationUseCase = authenticationUseCase;
        this.referentialUseCase = referentialUseCase;
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

        UUID parentFolderId = resolveParentFolderId(folder.getPath());
        setDefaultGroupIfNotSet(folder);

        String completePath = getCompletePath(folder);
        if (!existing.getCompletePath().equals(completePath) && folderRepository.existsByCompletePath(completePath)) {
            throw new ParamException("FOLDER_PATH_EXISTS", "A folder with path '" + completePath + "' already exists", "path");
        }

        existing.setName(folder.getName());
        existing.setPath(folder.getPath());
        existing.setParentId(parentFolderId);
        existing.setGroupIds(folder.getGroupIds());

        return folderRepository.save(existing);
    }

    private void validateFolderCreation(final Folder folder) {
        if (folder.getPath() == null || folder.getName() == null) {
            throw new ParamException("FOLDER_PATH_OR_NAME_NULL", "Folder path and name cannot be null", "path");
        }
        if (CollectionUtils.isNotEmpty(folder.getGroupIds()) && !referentialUseCase.hasCurrentUserWriteGroupAccess(folder.getGroupIds())) {
            throw new ParamException("FOLDER_GROUP_ACCESS_DENIED", "Current user does not have write access to the specified group", "groupId");
        }

        folder.setParentId(resolveParentFolderId(folder.getPath()));

        String completePath = getCompletePath(folder);
        if (folderRepository.existsByCompletePath(completePath)) {
            throw new ParamException("FOLDER_PATH_EXISTS", "A folder with path '" + completePath + "' already exists", "path");
        }
    }

    /**
     * Resolves the parent folder for a given path, requiring it to exist and the current user to have write access to it.
     */
    private UUID resolveParentFolderId(String path) {
        UUID parentFolderId = folderRepository.findFolderIdByCompletePath(path)
                .orElseThrow(() -> new ParamException("FOLDER_PARENT_NOT_FOUND", "Parent folder with path '" + path + "' does not exist", "path"));

        if (!folderRepository.hasCurrentUserWriteAccess(parentFolderId)) {
            throw new ParamException("FOLDER_PARENT_ACCESS_DENIED", "Current user does not have write access to the parent folder", "path");
        }
        return parentFolderId;
    }

    private void validateFolderUpdate(final Folder folder) {
        if (folder.getPath() == null || folder.getName() == null) {
            throw new ParamException("FOLDER_PATH_OR_NAME_NULL", "Folder path and name cannot be null", "path");
        }

        if (!folderRepository.hasCurrentUserWriteAccess(folder.getId())) {
            throw new ParamException("FOLDER_ACCESS_DENIED", "Current user does not have write access to the folder", "path");
        }

        if (CollectionUtils.isNotEmpty(folder.getGroupIds()) && !referentialUseCase.hasCurrentUserWriteGroupAccess(folder.getGroupIds())) {
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
