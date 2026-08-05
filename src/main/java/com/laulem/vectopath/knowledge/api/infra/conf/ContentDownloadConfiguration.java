package com.laulem.vectopath.knowledge.api.infra.conf;

import com.laulem.vectopath.knowledge.api.infra.properties.ContentDownloadProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class ContentDownloadConfiguration {

    /**
     * Redirects are never followed: only the originally validated URL goes through the
     * SSRF checks (allowed hosts, internal-network blocking), so a redirect target must not be trusted.
     */
    @Bean
    public RestClient contentDownloadRestClient(ContentDownloadProperties contentDownloadProperties) {
        HttpClientSettings settings = HttpClientSettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(contentDownloadProperties.getConnectTimeoutSeconds()))
                .withReadTimeout(Duration.ofSeconds(contentDownloadProperties.getTimeoutSeconds()))
                .withRedirects(HttpRedirects.DONT_FOLLOW);

        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect().build(settings);

        return RestClient.builder().requestFactory(requestFactory).build();
    }
}
