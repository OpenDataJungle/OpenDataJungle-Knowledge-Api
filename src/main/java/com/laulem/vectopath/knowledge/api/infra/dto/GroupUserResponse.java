package com.laulem.vectopath.knowledge.api.infra.dto;

import lombok.Data;

@Data
public class GroupUserResponse {
    private GroupResponse group;
    private PermissionResponse permission;
    private UserResponse user;
}
