package com.laulem.vectopath.knowledge.api.infra.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security.scopes")
@Data
public class SecurityScopesProperties {
    private Search search = new Search();
    private Resources resources = new Resources();
    private Folders folders = new Folders();

    @Data
    public static class Search {
        private String semantic;
    }

    @Data
    public static class Resources {
        private String read;
        private String write;
        private String delete;
    }

    @Data
    public static class Folders {
        private String read;
        private String write;
        private String delete;
    }
}
