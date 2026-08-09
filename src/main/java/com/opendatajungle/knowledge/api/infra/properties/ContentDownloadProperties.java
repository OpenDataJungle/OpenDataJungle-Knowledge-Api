package com.opendatajungle.knowledge.api.infra.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "content.download")
public class ContentDownloadProperties {
    private int timeoutSeconds;
    private int connectTimeoutSeconds;
    private long maxSizeBytes;
    private boolean blockInternalNetworks;
    private List<String> allowedHosts = List.of();
}
