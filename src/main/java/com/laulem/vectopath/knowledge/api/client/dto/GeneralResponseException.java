package com.laulem.vectopath.knowledge.api.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeneralResponseException(String code, String message, String path, String field,
                                       Map<String, String> information) {

}

