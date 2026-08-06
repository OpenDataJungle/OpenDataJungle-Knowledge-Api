package com.laulem.vectopath.knowledge.api.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.laulem.vectopath.knowledge.api.shared.util.CollectionUtils;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

public record CreateResourceRequest(
        @JsonProperty("name")
        String name,

        @JsonProperty("content")
        String content,

        @JsonProperty("url")
        String url,

        @JsonProperty("source_type")
        String sourceType,

        @JsonProperty("metadata")
        String metadata,

        @JsonProperty("folder_id")
        UUID folderId,

        @Valid
        @JsonProperty("group_permissions")
        List<ResourceGroupPermissionRequest> groupPermissions
) {

    public List<ResourceGroupPermissionRequest> groupPermissions() {
        return CollectionUtils.emptyIfNull(groupPermissions);
    }
}
