package com.opendatajungle.knowledge.api.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record FolderRequest(
        String name,
        String path,
        @JsonProperty("group_ids") List<UUID> groupIds
) {
}
