package com.laulem.vectopath.knowledge.api.client.service.resource.general;

import com.laulem.vectopath.knowledge.api.business.exception.ParamException;
import com.laulem.vectopath.knowledge.api.business.model.Resource;
import com.laulem.vectopath.knowledge.api.business.service.ResourceUseCase;
import com.laulem.vectopath.knowledge.api.client.dto.CreateResourceRequest;
import org.apache.logging.log4j.util.Strings;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Service
public class TextResourceGeneration implements GeneralResourceGeneration {
    private final ResourceUseCase resourceUseCase;

    public TextResourceGeneration(ResourceUseCase resourceUseCase) {
        this.resourceUseCase = resourceUseCase;
    }

    @Override
    public String getSourceType() {
        return "TEXT";
    }

    @Override
    public Resource processResource(Resource resource, CreateResourceRequest request) {
        validateInput(resource, request);
        resource.setContent(request.content());
        resource.setSourceType(getSourceType());
        resource.setSize((long) request.content().getBytes().length);
        resource.setContentType(MediaType.TEXT_PLAIN_VALUE);
        resource.setFolderId(request.folderId());
        return resourceUseCase.createResource(resource);
    }

    private void validateInput(Resource resource, CreateResourceRequest request) {
        if (Strings.isBlank(resource.getName())) {
            throw new ParamException("REQUIRED", "Resource name is required", "name");
        }
        if (Strings.isBlank(request.content())) {
            throw new ParamException("REQUIRED", "Content is required for TEXT resource type", "content");
        }
    }
}
