package com.laulem.vectopath.knowledge.api.business.model;

import com.laulem.vectopath.knowledge.api.shared.util.DateUtils;

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
    private AccessLevel accessLevel;
    private List<String> allowedRoles;
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

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(AccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }

    public List<String> getAllowedRoles() {
        return allowedRoles;
    }

    public void setAllowedRoles(List<String> allowedRoles) {
        this.allowedRoles = allowedRoles;
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

    public enum AccessLevel {
        PUBLIC, PRIVATE, ROLE_LIST
    }
}

