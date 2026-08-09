package com.opendatajungle.knowledge.api.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record SearchRequest(
        @JsonProperty(value = "query") String query,
        @JsonProperty(value = "limit", defaultValue = "10") Integer limit,
        @JsonProperty(value = "min_similarity", defaultValue = "0.50") Double minSimilarity,
        @JsonProperty("resource_ids") List<UUID> resourceIds
) {
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;
    private static final double DEFAULT_MIN_SIMILARITY = 0.5;

    public SearchRequest {
        if (limit == null || limit <= 0) limit = DEFAULT_LIMIT;
        if (limit > MAX_LIMIT) limit = MAX_LIMIT;
        if (minSimilarity == null || minSimilarity < 0 || minSimilarity > 1) minSimilarity = DEFAULT_MIN_SIMILARITY;
    }
}
