package com.opendatajungle.knowledge.api.business.model;

import com.opendatajungle.commons.util.DateUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Resource {
    private UUID id;
    private String name;
    private String content;
    private String contentType;
    private ResourceStatus status;
    private String metadata;
    private String sourceType;
    private String sourceName;
    private Long size;
    private String createdBy;
    private UUID folderId;
    private List<ResourceGroupPermission> groupPermissions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    public Resource() {
        this.status = ResourceStatus.PENDING;
        this.createdAt = DateUtils.now();
        this.updatedAt = DateUtils.now();
    }

    public Resource(String name, String content, String contentType, String metadata) {
        this();
        this.name = name;
        this.content = content;
        this.contentType = contentType;
        this.metadata = metadata;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
        this.updatedAt = DateUtils.now();
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(final Long size) {
        this.size = size;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getFolderId() {
        return folderId;
    }

    public void setFolderId(UUID folderId) {
        this.folderId = folderId;
    }

    public List<ResourceGroupPermission> getGroupPermissions() {
        return groupPermissions;
    }

    public void setGroupPermissions(List<ResourceGroupPermission> groupPermissions) {
        this.groupPermissions = groupPermissions;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

