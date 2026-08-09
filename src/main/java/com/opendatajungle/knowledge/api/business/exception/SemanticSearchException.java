package com.opendatajungle.knowledge.api.business.exception;

public class SemanticSearchException extends RuntimeException {

    public SemanticSearchException(Throwable cause) {
        super("Semantic search failed", cause);
    }
}
