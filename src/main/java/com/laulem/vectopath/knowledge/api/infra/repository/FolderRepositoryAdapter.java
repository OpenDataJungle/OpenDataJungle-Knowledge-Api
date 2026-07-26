package com.laulem.vectopath.knowledge.api.infra.repository;

import com.laulem.vectopath.knowledge.api.business.exception.NotFoundException;
import com.laulem.vectopath.knowledge.api.business.model.Folder;
import com.laulem.vectopath.knowledge.api.business.repository.FolderRepository;
import com.laulem.vectopath.knowledge.api.business.service.AuthenticationUseCase;
import com.laulem.vectopath.knowledge.api.infra.entity.FolderEntity;
import com.laulem.vectopath.knowledge.api.infra.entity.GroupEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class FolderRepositoryAdapter implements FolderRepository {
    private final FolderJpaRepository folderJpaRepository;
    private final AuthenticationUseCase authenticationUseCase;
    private final ReferentialRepository referentialRepository;

    public FolderRepositoryAdapter(FolderJpaRepository folderJpaRepository,
                                   AuthenticationUseCase authenticationUseCase, final ReferentialRepository referentialRepository) {
        this.authenticationUseCase = authenticationUseCase;
        this.folderJpaRepository = folderJpaRepository;
        this.referentialRepository = referentialRepository;
    }

    @Override
    @Transactional
    public Folder save(Folder folder) {
        FolderEntity entity = FolderEntity.fromDomain(folder);
        entity.setGroups(mapGroupEntities(folder));
        return folderJpaRepository.save(entity).toDomain();
    }

    private Set<GroupEntity> mapGroupEntities(final Folder folder) {
        Set<GroupEntity> groups = new HashSet<>();
        Optional.ofNullable(folder.getGroupIds()).orElse(List.of()).stream()
                .map(groupId -> {
                    GroupEntity groupEntity = new GroupEntity();
                    groupEntity.setId(groupId);
                    return groupEntity;
                }).forEach(groups::add);
        return groups;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Folder> findById(UUID id) {
        return folderJpaRepository.findById(id).map(FolderEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return folderJpaRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompletePath(String completePath) {
        return folderJpaRepository.existsByCompletePath(completePath);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Folder> findAll() {
        return folderJpaRepository.findAll().stream()
                .map(FolderEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        folderJpaRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasCurrentUserWriteAccess(UUID folderId) {
        FolderEntity folder = folderJpaRepository.findById(folderId)
                .orElseThrow(() -> new NotFoundException("Folder", folderId.toString()));
        if (authenticationUseCase.getCurrentUser().equals(folder.getCreatedBy())) {
            return true;
        }

        List<UUID> userGroupWriteAccess = referentialRepository.getGroupWriteAccess(authenticationUseCase.getCurrentUser());

        return folder
                .getGroups().stream()
                .map(GroupEntity::getId)
                .anyMatch(userGroupWriteAccess::contains);
    }

    @Override
    public boolean hasCurrentUserWriteGroupAccess(final List<UUID> groupIds) {
        return referentialRepository.hasGroupWriteAccess(groupIds, authenticationUseCase.getCurrentUser());
    }

    // TODO : Move to GroupRepositoryAdapted
    @Override
    public Optional<UUID> findFolderIdByCompletePath(final String completePath) {
        return folderJpaRepository.findByCompletePath(completePath)
                .map(FolderEntity::getId);
    }


    // TODO : Move to GroupRepositoryAdapted
    @Override
    @Transactional(readOnly = true)
    public List<UUID> getFolderGroupsIdByCompletePath(final String completePath) {
        return folderJpaRepository.findByCompletePath(completePath)
                .map(FolderEntity::getGroups)
                .map(groups -> groups.stream().map(GroupEntity::getId).toList())
                .orElse(List.of());
    }
}
