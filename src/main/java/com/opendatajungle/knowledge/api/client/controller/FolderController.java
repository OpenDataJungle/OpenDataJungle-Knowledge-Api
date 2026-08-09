package com.opendatajungle.knowledge.api.client.controller;

import com.opendatajungle.knowledge.api.business.model.Folder;
import com.opendatajungle.knowledge.api.business.service.AuthenticationUseCase;
import com.opendatajungle.knowledge.api.business.service.FolderUseCase;
import com.opendatajungle.knowledge.api.client.dto.FolderRequest;
import com.opendatajungle.knowledge.api.client.dto.FolderResponse;
import com.opendatajungle.knowledge.api.infra.conf.security.SecurityExpressions;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/folders")
public class FolderController {
    private final FolderUseCase folderUseCase;
    private final AuthenticationUseCase authenticationUseCase;

    public FolderController(FolderUseCase folderUseCase, AuthenticationUseCase authenticationUseCase) {
        this.folderUseCase = folderUseCase;
        this.authenticationUseCase = authenticationUseCase;
    }

    @PostMapping
    @PreAuthorize(SecurityExpressions.FOLDERS_WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public FolderResponse create(@Valid @RequestBody FolderRequest request) {
        Folder folder = new Folder(request.name(), request.path(), request.groupIds(), authenticationUseCase.getCurrentUser());
        Folder created = folderUseCase.create(folder);
        return toResponse(created);
    }

    @GetMapping
    @PreAuthorize(SecurityExpressions.FOLDERS_READ)
    public List<FolderResponse> listAll() {
        List<Folder> folders = folderUseCase.listAll();
        return folders.stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/me")
    @PreAuthorize(SecurityExpressions.FOLDERS_READ)
    public FolderResponse getMyDefaultFolder() {
        Folder folder = folderUseCase.getOrCreateDefaultFolder();
        return toResponse(folder);
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityExpressions.FOLDERS_READ)
    public FolderResponse getById(@PathVariable UUID id) {
        return toResponse(folderUseCase.getById(id));
    }

    @GetMapping("/{id}/children")
    @PreAuthorize(SecurityExpressions.FOLDERS_READ)
    public List<FolderResponse> findAllChildren(@PathVariable UUID id) {
        return folderUseCase.findAllChildren(id).stream()
                .map(this::toResponse)
                .toList();
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityExpressions.FOLDERS_WRITE)
    public FolderResponse update(@PathVariable UUID id, @Valid @RequestBody FolderRequest request) {
        Folder folder = new Folder();
        folder.setName(request.name());
        folder.setPath(request.path());
        folder.setGroupIds(request.groupIds());
        folder.setId(id);

        Folder updated = folderUseCase.update(folder);
        return toResponse(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityExpressions.FOLDERS_DELETE)
    public void delete(@PathVariable UUID id) {
        folderUseCase.delete(id);
    }

    private FolderResponse toResponse(Folder folder) {
        return new FolderResponse(
                folder.getId(),
                folder.getName(),
                folder.getPath(),
                folder.getCompletePath(),
                folder.getParentId(),
                folder.getGroupIds(),
                folder.getCreatedBy(),
                folder.getCreatedAt(),
                folder.getUpdatedAt()
        );
    }
}
