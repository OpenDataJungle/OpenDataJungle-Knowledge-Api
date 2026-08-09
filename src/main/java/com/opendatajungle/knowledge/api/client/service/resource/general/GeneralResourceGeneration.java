package com.opendatajungle.knowledge.api.client.service.resource.general;

import com.opendatajungle.knowledge.api.business.model.Resource;
import com.opendatajungle.knowledge.api.client.dto.CreateResourceRequest;

import java.io.IOException;

public interface GeneralResourceGeneration {
    String getSourceType();

    Resource processResource(Resource resource, CreateResourceRequest request) throws IOException;
}
