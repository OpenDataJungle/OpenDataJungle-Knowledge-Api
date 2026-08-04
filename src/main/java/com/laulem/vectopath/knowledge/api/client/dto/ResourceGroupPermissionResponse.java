package com.laulem.vectopath.knowledge.api.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.laulem.vectopath.knowledge.api.business.model.ResourceGroupPermission;

import java.util.UUID;

public record ResourceGroupPermissionResponse(
        @JsonProperty("group_id") UUID groupId,
        @JsonProperty("permission_id") UUID permissionId
) {
    public ResourceGroupPermissionResponse(ResourceGroupPermission groupPermission) {
        this(groupPermission.getGroupId(), groupPermission.getPermissionId());
    }
}
