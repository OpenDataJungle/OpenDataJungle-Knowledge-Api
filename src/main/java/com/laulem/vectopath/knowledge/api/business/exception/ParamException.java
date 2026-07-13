package com.laulem.vectopath.knowledge.api.business.exception;

import java.util.Map;

public class ParamException extends RuntimeException {

    private final String code;
    private final String field;
    private final Map<String, String> information;

    public ParamException(String code, String message, String field, Map<String, String> information) {
        super(message);
        this.code = code;
        this.field = field;
        this.information = information;
    }

    public ParamException(String code, String message, String field) {
        this(code, message, field, Map.of());
    }

    public String getCode() {
        return code;
    }

    public String getField() {
        return field;
    }

    public Map<String, String> getInformation() {
        return information;
    }
}
