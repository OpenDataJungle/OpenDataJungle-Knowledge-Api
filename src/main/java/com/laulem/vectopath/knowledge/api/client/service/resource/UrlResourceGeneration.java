package com.laulem.vectopath.knowledge.api.client.service.resource;

import com.laulem.vectopath.knowledge.api.business.exception.ParamException;
import com.laulem.vectopath.knowledge.api.business.model.Resource;
import com.laulem.vectopath.knowledge.api.business.service.ContentDownloaderUseCase;
import com.laulem.vectopath.knowledge.api.business.service.ResourceUseCase;
import com.laulem.vectopath.knowledge.api.client.dto.CreateResourceRequest;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UrlResourceGeneration implements GeneralResourceGeneration {
    private static final Logger logger = LoggerFactory.getLogger(UrlResourceGeneration.class);

    private final ResourceUseCase resourceUseCase;
    private final ContentDownloaderUseCase contentDownloaderUseCase;

    public UrlResourceGeneration(ResourceUseCase resourceUseCase,
                                 ContentDownloaderUseCase contentDownloaderUseCase) {
        this.resourceUseCase = resourceUseCase;
        this.contentDownloaderUseCase = contentDownloaderUseCase;
    }

    @Override
    public String getSourceType() {
        return "URL";
    }

    @Override
    public Resource processResource(Resource resource, CreateResourceRequest request) throws IOException {
        validateInput(resource, request);
        logger.info("Creating resource from URL: {}", request.url());
        String content = contentDownloaderUseCase.downloadContent(request.url().trim());
        resource.setSourceType(getSourceType());
        resource.setSourceName(request.url());
        resource.setContent(content);
        resource.setSize((long) content.getBytes().length);
        resource.setContentType(MediaType.TEXT_PLAIN_VALUE);
        return resourceUseCase.createResource(resource);
    }

    private void validateInput(Resource resource, CreateResourceRequest request) {
        if (Strings.isBlank(resource.getName())) {
            throw new ParamException("REQUIRED", "Resource name is required", "name");
        }
        if (Strings.isBlank(request.url())) {
            throw new ParamException("REQUIRED", "URL is required for URL resource type", "url");
        }
    }
}

