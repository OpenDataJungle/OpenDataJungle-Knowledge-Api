package com.laulem.vectopath.knowledge.api.business.service.splitter;

import com.laulem.vectopath.knowledge.api.business.model.Resource;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DocumentSplitterFactory {
    public static final String FILE_SOURCE_TYPE = "FILE";
    public static final String DEFAULT_KEY = "DEFAULT";

    private final Map<String, DocumentSplitter> splitterRegistry;

    public DocumentSplitterFactory(List<DocumentSplitter> splitters) {
        this.splitterRegistry = splitters.stream().collect(Collectors.toMap(DocumentSplitter::getSplitterKey, Function.identity()));
    }

    public DocumentSplitter getSplitter(Resource resource) {
        String sourceType = resource.getSourceType() != null ? resource.getSourceType().toUpperCase() : DEFAULT_KEY;

        if (FILE_SOURCE_TYPE.equals(sourceType)) {
            String specificFileKey = FILE_SOURCE_TYPE + "_" + extractExtension(resource);
            if (splitterRegistry.containsKey(specificFileKey)) {
                sourceType = specificFileKey;
            }
        }

        return splitterRegistry.getOrDefault(sourceType, splitterRegistry.get(DEFAULT_KEY));
    }

    private String extractExtension(Resource resource) {
        String sourceName = resource.getSourceName() != null ? resource.getSourceName() : "";
        int dotIndex = sourceName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == sourceName.length() - 1) {
            return "";
        }
        return sourceName.substring(dotIndex + 1).toUpperCase();
    }
}

