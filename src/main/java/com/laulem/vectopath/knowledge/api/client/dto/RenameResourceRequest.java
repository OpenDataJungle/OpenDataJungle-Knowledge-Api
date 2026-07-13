package com.laulem.vectopath.knowledge.api.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record RenameResourceRequest(
        @NotBlank(message = "Name must not be blank")
        @JsonProperty("name")
        String name
) {
}
