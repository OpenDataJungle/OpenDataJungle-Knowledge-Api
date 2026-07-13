package com.laulem.vectopath.knowledge.api.client.service;

import com.laulem.vectopath.knowledge.api.business.model.Resource;
import com.laulem.vectopath.knowledge.api.business.service.AuthenticationUseCase;
import com.laulem.vectopath.knowledge.api.client.dto.CreateResourceRequest;
import com.laulem.vectopath.knowledge.api.client.service.resource.FileResourceGenerationFactory;
import com.laulem.vectopath.knowledge.api.client.service.resource.GeneralResourceGenerationFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

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
        Objects.requireNonNull(file, "MultipartFile must not be null for FILE source type");
        Resource resource = buildBaseResource(request);
        return fileFactory.getResourceGeneration(file).processResource(resource, request, file);
    }

    private Resource buildBaseResource(CreateResourceRequest request) {
        String createdBy = authenticationUseCase.getCurrentUser();
        Resource.AccessLevel accessLevel = request.accessLevel() != null ? request.accessLevel() : Resource.AccessLevel.PRIVATE;

        Resource resource = new Resource();
        resource.setName(request.name());
        resource.setMetadata(request.metadata());
        resource.setCreatedBy(createdBy);
        resource.setAccessLevel(accessLevel);
        resource.setAllowedRoles(request.allowedRoles());
        return resource;
    }
}

