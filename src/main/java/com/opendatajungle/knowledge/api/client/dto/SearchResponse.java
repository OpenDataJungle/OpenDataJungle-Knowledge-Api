package com.opendatajungle.knowledge.api.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opendatajungle.knowledge.api.business.model.PartialResource;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchResponse(
        @JsonProperty("vector_id") UUID vectorId,
        @JsonProperty("resource_id") UUID resourceId,
        @JsonProperty("resource_name") String resourceName,
        @JsonProperty("content") String content,
        @JsonProperty("content_type") String contentType,
        @JsonProperty("metadata") String metadata,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt,
        @JsonProperty("similarity_score") Double similarityScore
) {
    public SearchResponse(PartialResource partialResource) {
        this(
                partialResource.getVectorId(),
                partialResource.getResourceId(),
                partialResource.getResourceName(),
                partialResource.getContent(),
                partialResource.getContentType(),
                partialResource.getMetadata(),
                partialResource.getCreatedAt(),
                partialResource.getUpdatedAt(),
                partialResource.getSimilarityScore()
        );
    }
}
