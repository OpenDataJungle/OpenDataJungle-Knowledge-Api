package com.laulem.vectopath.knowledge.api.business.exception;
public class HttpDownloadException extends RuntimeException {
    public HttpDownloadException(int statusCode, String url) {
        super("HTTP error " + statusCode + " when downloading from " + url);
    }
}
