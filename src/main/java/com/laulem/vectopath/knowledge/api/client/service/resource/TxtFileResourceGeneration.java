package com.laulem.vectopath.knowledge.api.client.service.resource;

import com.laulem.vectopath.knowledge.api.business.exception.ParamException;
import com.laulem.vectopath.knowledge.api.business.model.Resource;
import com.laulem.vectopath.knowledge.api.business.service.ResourceUseCase;
import com.laulem.vectopath.knowledge.api.client.dto.CreateResourceRequest;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class TxtFileResourceGeneration implements FileResourceGeneration {
    private final ResourceUseCase resourceUseCase;

    public TxtFileResourceGeneration(ResourceUseCase resourceUseCase) {
        this.resourceUseCase = resourceUseCase;
    }

    @Override
    public String getFileExtension() {
        return "TXT";
    }

    @Override
    public Resource processResource(Resource resource, CreateResourceRequest request, MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        validateInput(resource, content);
        resource.setSourceType("FILE");
        resource.setSourceName(file.getOriginalFilename());
        resource.setContent(content);
        resource.setSize(file.getSize());
        resource.setContentType(MediaType.TEXT_PLAIN_VALUE);
        return resourceUseCase.createResource(resource);
    }

    private void validateInput(Resource resource, String content) {
        if (Strings.isBlank(resource.getName())) {
            throw new ParamException("REQUIRED", "Resource name is required", "name");
        }
        if (Strings.isBlank(content)) {
            throw new ParamException("REQUIRED", "File content is required for FILE resource type", "file");
        }
    }
}

