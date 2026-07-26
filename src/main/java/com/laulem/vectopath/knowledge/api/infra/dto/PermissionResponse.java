package com.laulem.vectopath.knowledge.api.infra.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class PermissionResponse {
    private UUID id;
    private String name;
    private String description;
    private Boolean canRead;
    private Boolean canWrite;
    private Boolean isAdmin;
}
