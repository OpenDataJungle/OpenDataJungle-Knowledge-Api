package com.laulem.vectopath.knowledge.api.infra.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "embedding")
public class EmbeddingProperties {
    private EmbeddingProviderType provider = EmbeddingProviderType.OPENAI;
    private String apiKey;
    private String baseUrl;
    private String model;
    private Integer dimensions;
}
