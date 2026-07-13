package com.laulem.vectopath.knowledge.api.client.service.resource;

import com.laulem.vectopath.knowledge.api.business.model.Resource;
import com.laulem.vectopath.knowledge.api.client.dto.CreateResourceRequest;

import java.io.IOException;

public interface GeneralResourceGeneration {
    String getSourceType();
    Resource processResource(Resource resource, CreateResourceRequest request) throws IOException;
}
