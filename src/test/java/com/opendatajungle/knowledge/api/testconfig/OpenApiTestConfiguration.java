package com.opendatajungle.knowledge.api.testconfig;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class OpenApiTestConfiguration {

    @Bean
    public OpenAPI openApi(@Value("${application.title}") String title,
                           @Value("${application.version}") String version) {
        return new OpenAPI().info(new Info().title(title).version(version));
    }
}

