package com.laulem.vectopath.knowledge.api.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class FolderRequest {
    private String name;
    private String path;

    @JsonProperty("group_ids")
    private List<UUID> groupIds;
}
