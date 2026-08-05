package com.laulem.vectopath.knowledge.api.business.exception;

public class ContentDownloadException extends RuntimeException {

    public ContentDownloadException(String url, Throwable cause) {
        super("Download interrupted for " + url, cause);
    }
}
