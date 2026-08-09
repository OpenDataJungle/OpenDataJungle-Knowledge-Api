package com.opendatajungle.knowledge.api.client.controller;

import com.opendatajungle.knowledge.api.business.exception.NotFoundException;
import com.opendatajungle.knowledge.api.business.model.ResourceStatus;
import com.opendatajungle.knowledge.api.business.service.ResourceUseCase;
import com.opendatajungle.knowledge.api.client.dto.CreateResourceRequest;
import com.opendatajungle.knowledge.api.client.dto.RenameResourceRequest;
import com.opendatajungle.knowledge.api.client.dto.ResourceContentResponse;
import com.opendatajungle.knowledge.api.client.dto.ResourceGroupPermissionRequest;
import com.opendatajungle.knowledge.api.client.dto.ResourceResponse;
import com.opendatajungle.knowledge.api.client.service.ResourceCreationService;
import com.opendatajungle.knowledge.api.infra.conf.security.SecurityExpressions;
import com.opendatajungle.knowledge.api.shared.util.CollectionUtils;
import com.opendatajungle.knowledge.api.shared.util.StringUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/v1/resources")
public class ResourceController {
    private static final Logger logger = LoggerFactory.getLogger(ResourceController.class);

    private final ResourceUseCase resourceUseCase;
    private final ResourceCreationService resourceCreationService;

    public ResourceController(ResourceUseCase resourceUseCase,
                              ResourceCreationService resourceCreationService) {
        this.resourceUseCase = resourceUseCase;
        this.resourceCreationService = resourceCreationService;
    }

    @PreAuthorize(SecurityExpressions.RESOURCES_WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResourceResponse createResource(@RequestBody @Validated CreateResourceRequest request) throws IOException {
        return new ResourceResponse(resourceCreationService.createGeneralResource(request));
    }

    @PreAuthorize(SecurityExpressions.RESOURCES_WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResourceResponse createResourceFromFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam(value = "metadata", required = false) String metadata,
            @RequestParam(value = "folder_id", required = false) UUID folderId,
            @Valid @RequestPart(value = "group_permissions", required = false) List<ResourceGroupPermissionRequest> groupPermissions) throws IOException {
        logger.info("Creating resource from file: {} with name: {}",
                StringUtils.sanitizeForLog(file.getOriginalFilename()), StringUtils.sanitizeForLog(name));
        CreateResourceRequest request = new CreateResourceRequest(name, null, null, "file", metadata, folderId, CollectionUtils.emptyIfNull(groupPermissions));
        return new ResourceResponse(resourceCreationService.createFileResource(request, file));
    }

    @PreAuthorize(SecurityExpressions.RESOURCES_READ)
    @GetMapping
    public List<ResourceResponse> findAll() {
        logger.info("Retrieving all resources");
        return resourceUseCase.findAll()
                .stream()
                .map(ResourceResponse::new)
                .toList();
    }

    @PreAuthorize(SecurityExpressions.RESOURCES_READ)
    @GetMapping("/{id}")
    public ResourceResponse getResourceById(@PathVariable UUID id) {
        logger.info("Retrieving resource: {}", id);

        return resourceUseCase.findById(id)
                .map(ResourceResponse::new)
                .orElseThrow(() -> new NotFoundException("Resource", id.toString()));
    }

    @PreAuthorize(SecurityExpressions.RESOURCES_READ)
    @GetMapping("/search")
    public List<ResourceResponse> searchResources(@RequestParam(required = false) String name,
                                                   @RequestParam(required = false) String path) {
        logger.info("Searching resources by name: {} and path: {}",
                StringUtils.sanitizeForLog(name), StringUtils.sanitizeForLog(path));

        return resourceUseCase.searchResources(name, path)
                .stream()
                .map(ResourceResponse::new)
                .toList();
    }

    @PreAuthorize(SecurityExpressions.RESOURCES_READ)
    @GetMapping("/status/{status}")
    public List<ResourceResponse> findByStatus(@PathVariable ResourceStatus status) {
        logger.info("Retrieving resources by status: {}", status);

        return resourceUseCase.findByStatus(status)
                .stream()
                .map(ResourceResponse::new)
                .toList();
    }

    @PreAuthorize(SecurityExpressions.RESOURCES_READ)
    @GetMapping("/{id}/content")
    public ResourceContentResponse getResourceContent(@PathVariable UUID id) {
        logger.info("Retrieving content for resource: {}", id);

        return resourceUseCase.findById(id)
                .map(ResourceContentResponse::new)
                .orElseThrow(() -> new NotFoundException("Resource", id.toString()));
    }

    @PreAuthorize(SecurityExpressions.RESOURCES_WRITE)
    @PostMapping("/{id}/reprocess")
    public ResourceResponse reprocessResource(@PathVariable UUID id) {
        logger.info("Reprocessing resource: {}", id);
        return new ResourceResponse(resourceUseCase.reprocessResource(id));
    }

    @PreAuthorize(SecurityExpressions.RESOURCES_WRITE)
    @PatchMapping("/{id}")
    public void renameResource(@PathVariable UUID id,
                               @RequestBody @Validated RenameResourceRequest request) {
        // TODO : Add the possibilities of setting group permissions and folder_id
        logger.info("Renaming resource {} to: {}", id, StringUtils.sanitizeForLog(request.name()));
        resourceUseCase.renameResource(id, request.name());
    }

    @PreAuthorize(SecurityExpressions.RESOURCES_DELETE)
    @DeleteMapping("/{id}")
    public void deleteResource(@PathVariable UUID id) {
        logger.info("Deleting resource: {}", id);
        resourceUseCase.deleteResource(id);
    }
}
