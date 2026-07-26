package com.laulem.vectopath.knowledge.api.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.laulem.vectopath.knowledge.api.business.model.Resource;
import com.laulem.vectopath.knowledge.api.business.model.ResourceStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResourceResponse(
        @JsonProperty("id") UUID id,
        @JsonProperty("name") String name,
        @JsonProperty("content_type") String contentType,
        @JsonProperty("status") ResourceStatus status,
        @JsonProperty("metadata") String metadata,
        @JsonProperty("source_type") String sourceType,
        @JsonProperty("source_name") String sourceName,
        @JsonProperty("size") Long size,
        @JsonProperty("created_by") String createdBy,
        @JsonProperty("folder_id") UUID folderId,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt
) {
    public ResourceResponse(Resource resource) {
        this(
                resource.getId(),
                resource.getName(),
                resource.getContentType(),
                resource.getStatus(),
                resource.getMetadata(),
                resource.getSourceType(),
                resource.getSourceName(),
                resource.getSize(),
                resource.getCreatedBy(),
                resource.getFolderId(),
                resource.getCreatedAt(),
                resource.getUpdatedAt()
        );
    }
}

