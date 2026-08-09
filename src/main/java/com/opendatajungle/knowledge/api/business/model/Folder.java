package com.opendatajungle.knowledge.api.business.model;

import com.opendatajungle.knowledge.api.shared.util.DateUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Folder {
    private UUID id;
    private String name;
    private String path;
    private UUID parentId;
    private List<UUID> groupIds;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Folder() {
        this.createdAt = DateUtils.now();
        this.updatedAt = DateUtils.now();
    }

    public Folder(String name, String path, List<UUID> groupIds, String createdBy) {
        this();
        this.name = name;
        this.path = path;
        this.groupIds = groupIds;
        this.createdBy = createdBy;
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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

    public String getCompletePath() {
        return path + "/" + name;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public List<UUID> getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(List<UUID> groupIds) {
        this.groupIds = groupIds;
    }
}
