package com.laulem.vectopath.knowledge.api.infra.conf;

import com.laulem.vectopath.knowledge.api.business.service.RerankerUseCase;
import com.laulem.vectopath.knowledge.api.infra.properties.RerankerProperties;
import com.laulem.vectopath.knowledge.api.infra.service.DefaultRerankerService;
import com.laulem.vectopath.knowledge.api.infra.service.HuggingFaceRerankerService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RerankerConfiguration {

    @Bean
    @ConditionalOnProperty(name = "reranker.type", havingValue = "HUGGINGFACE")
    public RerankerUseCase huggingFaceRerankerService(RerankerProperties properties, RestClient.Builder restClientBuilder) {
        return new HuggingFaceRerankerService(restClientBuilder.baseUrl(properties.getBaseUrl()).build());
    }

    @Bean
    @ConditionalOnMissingBean(RerankerUseCase.class)
    public RerankerUseCase defaultRerankerService() {
        return new DefaultRerankerService();
    }
}
