package com.opendatajungle.knowledge.api.infra.dto;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class UserResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String username;
    private Instant createdAt;
    private Instant updatedAt;
}
