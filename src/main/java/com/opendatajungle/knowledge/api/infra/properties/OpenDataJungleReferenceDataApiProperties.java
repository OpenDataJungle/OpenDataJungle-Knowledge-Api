package com.opendatajungle.knowledge.api.infra.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "open-data-jungle.reference-data-api")
public class OpenDataJungleReferenceDataApiProperties {
    private String baseUrl;
    private String userByUsername;
    private String userGroupsByUserId;
}
