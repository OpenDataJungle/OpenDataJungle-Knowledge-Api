package com.laulem.vectopath.knowledge.api.client.service.resource;

import com.laulem.vectopath.knowledge.api.business.model.Resource;
import com.laulem.vectopath.knowledge.api.client.dto.CreateResourceRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileResourceGeneration {
    String getFileExtension();
    Resource processResource(Resource resource, CreateResourceRequest request, MultipartFile file) throws IOException;
}
