package com.opendatajungle.knowledge.api.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ResourceGroupPermissionRequest(
        @NotNull(message = "Group id must not be null")
        @JsonProperty("group_id")
        UUID groupId,

        @NotNull(message = "Permission id must not be null")
        @JsonProperty("permission_id")
        UUID permissionId
) {
}
