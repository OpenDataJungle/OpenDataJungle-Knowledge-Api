package com.laulem.vectopath.knowledge.api.client.exception;

public class UnsupportedSourceTypeException extends RuntimeException {

    public UnsupportedSourceTypeException(String sourceType) {
        super("Unsupported source type: " + sourceType);
    }
}

