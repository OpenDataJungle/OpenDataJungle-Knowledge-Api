package com.opendatajungle.knowledge.api.business.model;

import java.time.Instant;
import java.util.UUID;

public class PartialResource {
    private UUID vectorId;
    private String content;
    private UUID resourceId;
    private String resourceName;
    private String contentType;
    private String metadata;
    private Instant createdAt;
    private Instant updatedAt;
    private Double similarityScore;

    // Getters & Setters
    public UUID getVectorId() {
        return vectorId;
    }

    public void setVectorId(UUID vectorId) {
        this.vectorId = vectorId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public void setResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }


    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(Double similarityScore) {
        this.similarityScore = similarityScore;
    }
}
