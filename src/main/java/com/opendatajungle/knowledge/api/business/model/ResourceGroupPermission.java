package com.opendatajungle.knowledge.api.business.model;

import java.util.UUID;

public class ResourceGroupPermission {
    private UUID groupId;
    private UUID permissionId;

    public ResourceGroupPermission() {
    }

    public ResourceGroupPermission(UUID groupId, UUID permissionId) {
        this.groupId = groupId;
        this.permissionId = permissionId;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public void setGroupId(UUID groupId) {
        this.groupId = groupId;
    }

    public UUID getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(UUID permissionId) {
        this.permissionId = permissionId;
    }
}
