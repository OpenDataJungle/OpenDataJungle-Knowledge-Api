package com.laulem.vectopath.knowledge.api.business.exception;

public class DownloadInterruptedException extends RuntimeException {

    public DownloadInterruptedException(String url, Throwable cause) {
        super("Download interrupted for " + url, cause);
    }
}
