package com.laulem.vectopath.infra.conf;

import com.laulem.vectopath.business.service.RerankerService;
import com.laulem.vectopath.infra.properties.RerankerProperties;
import com.laulem.vectopath.infra.service.HuggingFaceRerankerServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(name = "reranker.type", havingValue = "HUGGINGFACE")
public class HuggingFaceRerankerConfiguration {
    @Bean
    public RerankerService rerankerService(RerankerProperties properties) {
        return new HuggingFaceRerankerServiceImpl(RestClient.builder().baseUrl(properties.getBaseUrl()).build());
    }
}
