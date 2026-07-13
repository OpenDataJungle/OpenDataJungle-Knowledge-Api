package com.laulem.vectopath.knowledge.api.infra.conf;

import com.laulem.vectopath.knowledge.api.infra.properties.EmbeddingProperties;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfiguration {
    private final EmbeddingProperties embeddingProperties;

    public EmbeddingConfiguration(EmbeddingProperties embeddingProperties) {
        this.embeddingProperties = embeddingProperties;
    }

    @Bean
    @ConditionalOnProperty(name = "embedding.provider", havingValue = "OPENAI", matchIfMissing = true)
    public EmbeddingModel openAiEmbeddingModel() {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(embeddingProperties.getApiKey())
                .build();

        OpenAiEmbeddingOptions.Builder options = OpenAiEmbeddingOptions.builder()
                .model(embeddingProperties.getModel());

        if (embeddingProperties.getDimensions() != null) {
            options.dimensions(embeddingProperties.getDimensions());
        }

        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options.build());
    }

    @Bean
    @ConditionalOnProperty(name = "embedding.provider", havingValue = "OLLAMA")
    public EmbeddingModel ollamaEmbeddingModel() {
        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(embeddingProperties.getBaseUrl())
                .build();

        OllamaEmbeddingOptions.Builder options = OllamaEmbeddingOptions.builder()
                .model(embeddingProperties.getModel());

        if (embeddingProperties.getDimensions() != null) {
            options.dimensions(embeddingProperties.getDimensions());
        }

        return OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(options.build())
                .build();
    }
}
