package com.opendatajungle.knowledge.api.client.exception;

import com.opendatajungle.commons.util.StringUtils;

public class UnsupportedFileExtensionException extends RuntimeException {

    public UnsupportedFileExtensionException(String extension) {
        super("Unsupported file extension: " + StringUtils.sanitizeForLog(extension));
    }
}
