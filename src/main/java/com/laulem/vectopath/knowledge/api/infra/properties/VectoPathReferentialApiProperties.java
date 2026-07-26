package com.laulem.vectopath.knowledge.api.infra.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "vecto-path.referential-api")
public class VectoPathReferentialApiProperties {
    private String baseUrl;
    private String userByUsername;
    private String userGroupsByUserId;
}
