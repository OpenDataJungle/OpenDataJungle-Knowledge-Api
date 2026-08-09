package com.opendatajungle.knowledge.api.business.service;

import java.io.IOException;

public interface ContentDownloaderUseCase {
    String downloadContent(String url) throws IOException;
}
