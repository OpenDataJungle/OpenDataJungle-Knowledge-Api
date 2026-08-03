package com.laulem.vectopath.knowledge.api.business.exception;

public class SemanticSearchException extends RuntimeException {

    public SemanticSearchException(Throwable cause) {
        super("Semantic search failed", cause);
    }
}
