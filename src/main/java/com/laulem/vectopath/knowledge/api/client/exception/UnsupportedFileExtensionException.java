package com.laulem.vectopath.knowledge.api.client.exception;

public class UnsupportedFileExtensionException extends RuntimeException {

    public UnsupportedFileExtensionException(String extension) {
        super("Unsupported file extension: " + extension);
    }
}
