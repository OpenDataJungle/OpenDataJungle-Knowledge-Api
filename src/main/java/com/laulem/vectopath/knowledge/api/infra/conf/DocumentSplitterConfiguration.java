package com.laulem.vectopath.knowledge.api.infra.conf;

import com.laulem.vectopath.knowledge.api.business.service.splitter.DocumentSplitter;
import com.laulem.vectopath.knowledge.api.business.service.splitter.DocumentSplitterFactory;
import com.laulem.vectopath.knowledge.api.infra.service.DefaultTokenTextSplitter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DocumentSplitterConfiguration {

    @Bean("defaultDocumentSplitter")
    @ConditionalOnMissingBean(name = "defaultDocumentSplitter")
    public DocumentSplitter defaultDocumentSplitter() {
        return new DefaultTokenTextSplitter();
    }

    @Bean
    public DocumentSplitterFactory documentSplitterFactory(List<DocumentSplitter> splitters) {
        return new DocumentSplitterFactory(splitters);
    }
}
