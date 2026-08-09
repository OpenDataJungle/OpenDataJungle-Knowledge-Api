package com.opendatajungle.knowledge.api.client.exception;

import com.opendatajungle.knowledge.api.shared.util.StringUtils;

public class UnsupportedFileExtensionException extends RuntimeException {

    public UnsupportedFileExtensionException(String extension) {
        super("Unsupported file extension: " + StringUtils.sanitizeForLog(extension));
    }
}
