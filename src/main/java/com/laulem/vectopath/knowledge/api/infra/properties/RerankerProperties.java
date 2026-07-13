package com.laulem.vectopath.knowledge.api.infra.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "reranker")
public class RerankerProperties {
    private String type;
    private String baseUrl;
}
