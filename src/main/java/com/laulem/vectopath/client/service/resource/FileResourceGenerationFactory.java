package com.laulem.vectopath.client.service.resource;

import com.laulem.vectopath.client.exception.UnsupportedFileExtensionException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FileResourceGenerationFactory {
    private final Map<String, FileResourceGeneration> resourceGenerationFactory;

    public FileResourceGenerationFactory(List<FileResourceGeneration> generations) {
        this.resourceGenerationFactory = generations.stream()
                .collect(Collectors.toMap(FileResourceGeneration::getFileExtension, Function.identity(), (_, replacement) -> replacement));
    }

    public FileResourceGeneration getResourceGeneration(MultipartFile file) {
        FileResourceGeneration fileResourceGeneration = resourceGenerationFactory.get(extractExtension(file));
        if (fileResourceGeneration == null) {
            throw new UnsupportedFileExtensionException(extractExtension(file));
        }
        return fileResourceGeneration;
    }

    private String extractExtension(MultipartFile file) {
        String filename = Objects.requireNonNull(file.getOriginalFilename(), "File name must not be null");
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            throw new UnsupportedFileExtensionException("(no extension)");
        }
        return filename.substring(dotIndex + 1).toUpperCase();
    }
}

