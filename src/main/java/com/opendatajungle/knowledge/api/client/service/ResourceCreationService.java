package com.opendatajungle.knowledge.api.client.service;

import com.opendatajungle.commons.business.exception.ParamException;
import com.opendatajungle.knowledge.api.business.model.Resource;
import com.opendatajungle.knowledge.api.business.model.ResourceGroupPermission;
import com.opendatajungle.commons.business.service.AuthenticationUseCase;
import com.opendatajungle.knowledge.api.client.dto.CreateResourceRequest;
import com.opendatajungle.knowledge.api.client.dto.ResourceGroupPermissionRequest;
import com.opendatajungle.knowledge.api.client.service.resource.files.FileResourceGenerationFactory;
import com.opendatajungle.knowledge.api.client.service.resource.general.GeneralResourceGenerationFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ResourceCreationService {
    public static final String DEFAULT_SOURCE_TYPE = "TEXT";

    private final GeneralResourceGenerationFactory generalFactory;
    private final FileResourceGenerationFactory fileFactory;
    private final AuthenticationUseCase authenticationUseCase;

    public ResourceCreationService(GeneralResourceGenerationFactory generalFactory,
                                   FileResourceGenerationFactory fileFactory,
                                   AuthenticationUseCase authenticationUseCase) {
        this.generalFactory = generalFactory;
        this.fileFactory = fileFactory;
        this.authenticationUseCase = authenticationUseCase;
    }

    public Resource createGeneralResource(CreateResourceRequest request) throws IOException {
        String sourceType = StringUtils.hasText(request.sourceType()) ? request.sourceType().toUpperCase() : DEFAULT_SOURCE_TYPE;
        Resource resource = buildBaseResource(request);
        return generalFactory.getResourceGeneration(sourceType).processResource(resource, request);
    }

    public Resource createFileResource(CreateResourceRequest request, MultipartFile file) throws IOException {
        if (file == null) {
            throw new ParamException("REQUIRED", "Uploaded file must not be null", "file");
        }

        if (file.isEmpty()) {
            throw new ParamException("REQUIRED", "Uploaded file must not be empty", "file");
        }

        Resource resource = buildBaseResource(request);
        return fileFactory.getResourceGeneration(file).processResource(resource, request, file);
    }

    private Resource buildBaseResource(CreateResourceRequest request) {
        Resource resource = new Resource();
        resource.setName(request.name());
        resource.setMetadata(request.metadata());
        resource.setCreatedBy(authenticationUseCase.getCurrentUser());
        resource.setGroupPermissions(toGroupPermissions(request.groupPermissions()));
        return resource;
    }

    private List<ResourceGroupPermission> toGroupPermissions(List<ResourceGroupPermissionRequest> groupPermissions) {
        return Optional.ofNullable(groupPermissions).orElse(Collections.emptyList()).stream()
                .filter(Objects::nonNull)
                .map(groupPermission -> new ResourceGroupPermission(groupPermission.groupId(), groupPermission.permissionId()))
                .toList();
    }
}

