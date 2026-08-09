package com.opendatajungle.knowledge.api.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class FolderResponse {
    private UUID id;
    private String name;
    private String path;
    private String completePath;
    private UUID parentId;
    private List<UUID> groupIds;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
