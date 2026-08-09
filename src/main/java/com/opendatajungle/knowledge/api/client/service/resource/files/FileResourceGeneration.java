package com.opendatajungle.knowledge.api.client.service.resource.files;

import com.opendatajungle.knowledge.api.business.model.Resource;
import com.opendatajungle.knowledge.api.client.dto.CreateResourceRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileResourceGeneration {
    String getFileExtension();

    Resource processResource(Resource resource, CreateResourceRequest request, MultipartFile file) throws IOException;
}
