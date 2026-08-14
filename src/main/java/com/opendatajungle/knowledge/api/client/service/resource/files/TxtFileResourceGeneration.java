package com.opendatajungle.knowledge.api.client.service.resource.files;

import com.opendatajungle.commons.business.exception.ParamException;
import com.opendatajungle.knowledge.api.business.model.Resource;
import com.opendatajungle.knowledge.api.business.service.ResourceUseCase;
import com.opendatajungle.knowledge.api.client.dto.CreateResourceRequest;
import org.apache.logging.log4j.util.Strings;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

@Order(Ordered.HIGHEST_PRECEDENCE)
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
        validateInput(resource, content, file);
        resource.setSourceType("FILE");
        resource.setSourceName(sanitizeFileName(file.getOriginalFilename()));
        resource.setContent(content);
        resource.setSize(file.getSize());
        resource.setContentType(MediaType.TEXT_PLAIN_VALUE);
        resource.setFolderId(request.folderId());
        return resourceUseCase.createResource(resource);
    }

    /**
     * Keeps only the file base name so a crafted client cannot inject a path into the stored metadata.
     */
    private String sanitizeFileName(String originalFilename) {
        if (Strings.isBlank(originalFilename)) {
            throw new ParamException("REQUIRED", "File name is required", "file");
        }

        String sanitizedFileName = Paths.get(originalFilename.replace('\\', '/')).getFileName().toString();
        if (Strings.isBlank(sanitizedFileName)) {
            throw new ParamException("REQUIRED", "File name is required", "file");
        }

        return sanitizedFileName;
    }

    private void validateInput(Resource resource, String content, final MultipartFile file) {
        if (Strings.isBlank(resource.getName())) {
            throw new ParamException("REQUIRED", "Resource name is required", "name");
        }
        if (Strings.isBlank(content)) {
            throw new ParamException("REQUIRED", "File content is required for FILE resource type", "file");
        }

        if (file == null || file.isEmpty()) {
            throw new ParamException("REQUIRED", "Uploaded file must not be null or empty", "file");
        }
    }
}

