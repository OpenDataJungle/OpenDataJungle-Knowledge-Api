package com.laulem.vectopath.knowledge.api.client.exception;

import com.laulem.vectopath.knowledge.api.shared.util.StringUtils;

public class UnsupportedFileExtensionException extends RuntimeException {

    public UnsupportedFileExtensionException(String extension) {
        super("Unsupported file extension: " + StringUtils.sanitizeForLog(extension));
    }
}
