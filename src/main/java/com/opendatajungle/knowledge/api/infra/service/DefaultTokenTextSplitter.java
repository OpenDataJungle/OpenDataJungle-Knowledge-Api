package com.opendatajungle.knowledge.api.infra.service;

import com.opendatajungle.knowledge.api.business.service.splitter.DocumentSplitter;
import com.opendatajungle.knowledge.api.business.service.splitter.DocumentSplitterFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import java.util.List;

public class DefaultTokenTextSplitter implements DocumentSplitter {
    private final TokenTextSplitter tokenTextSplitter;

    public DefaultTokenTextSplitter() {
        this.tokenTextSplitter = TokenTextSplitter.builder().build();
    }

    @Override
    public List<String> split(String content) {
        List<Document> split = tokenTextSplitter.apply(List.of(new Document(content)));
        return split.stream()
                .map(Document::getText)
                .toList();
    }

    @Override
    public String getSplitterKey() {
        return DocumentSplitterFactory.DEFAULT_KEY;
    }
}
