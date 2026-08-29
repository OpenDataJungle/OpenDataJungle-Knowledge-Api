package com.opendatajungle.knowledge.api.infra.dto;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class GroupResponse {
    private UUID id;
    private String name;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}
