package com.laulem.vectopath.knowledge.api.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.laulem.vectopath.knowledge.api.business.model.Resource;

import java.util.UUID;

public record ResourceContentResponse(
        @JsonProperty("id") UUID id,
        @JsonProperty("name") String name,
        @JsonProperty("content") String content
) {
    public ResourceContentResponse(Resource resource) {
        this(resource.getId(), resource.getName(), resource.getContent());
    }
}
