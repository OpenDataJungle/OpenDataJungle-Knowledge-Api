package com.laulem.vectopath.knowledge.api.client.exception;

public class UnsupportedFileExtensionException extends RuntimeException {

    public UnsupportedFileExtensionException(String extension) { // TODO : Add to Exception handler
        super("Unsupported file extension: " + extension);
    }
}
