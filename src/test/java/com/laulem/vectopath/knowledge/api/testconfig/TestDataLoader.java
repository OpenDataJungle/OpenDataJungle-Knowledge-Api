package com.laulem.vectopath.knowledge.api.testconfig;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TestDataLoader {
    public static final int EMBEDDING_DIMENSIONS = 1536;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JdbcTemplate jdbcTemplate;

    public TestDataLoader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void loadTestData(String resourcesFileName, String vectorsFileName) {
        try {
            JsonNode resourcesData = objectMapper.readTree(new ClassPathResource(resourcesFileName).getInputStream());

            for (JsonNode resource : resourcesData) {
                insertResource(resource);
            }

            JsonNode vectorsData = objectMapper.readTree(new ClassPathResource(vectorsFileName).getInputStream());
            String embeddingVector = createDefaultEmbeddingStringVector();
            for (JsonNode vector : vectorsData) {
                insertVector(vector, embeddingVector);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load test data from JSON files: " + resourcesFileName + ", " + vectorsFileName, e);
        }
    }

    public void loadTestDefaultData() {
        loadTestData("test-data/test-resources.json", "test-data/test-vectors.json");
    }

    public void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM knowledge.vector_store");
        jdbcTemplate.execute("DELETE FROM knowledge.resources");
    }

    private void insertResource(JsonNode resource) {
        String resourceSql = """
                INSERT INTO knowledge.resources (id, name, content, content_type, status, metadata, source_type, source_name, created_by, access_level, created_at, updated_at)
                VALUES (?::uuid, ?, ?, ?, ?, ?::json, ?, ?, ?, ?, NOW(), NOW())
                """;

        jdbcTemplate.update(resourceSql,
                resource.get("id").asText(),
                resource.get("name").asText(),
                resource.get("content").asText(),
                resource.get("contentType").asText(),
                resource.get("status").asText(),
                resource.get("metadata").asText(),
                resource.has("sourceType") && !resource.get("sourceType").isNull() ? resource.get("sourceType").asText() : null,
                resource.has("sourceName") && !resource.get("sourceName").isNull() ? resource.get("sourceName").asText() : null,
                resource.has("createdBy") && !resource.get("createdBy").isNull() ? resource.get("createdBy").asText() : null,
                resource.has("accessLevel") && !resource.get("accessLevel").isNull() ? resource.get("accessLevel").asText() : null);
    }

    private void insertVector(JsonNode vector, String embeddingVector) {
        String sql = """
                INSERT INTO knowledge.vector_store (id, content, metadata, embedding)
                VALUES (?, ?, ?::jsonb, ?::vector)
                """;

        jdbcTemplate.update(sql,
                vector.get("id").asText(),
                vector.get("content").asText(),
                vector.get("metadata").asText(),
                embeddingVector);
    }

    public String createDefaultEmbeddingStringVector() {
        StringBuilder vectorStr = new StringBuilder("[");
        for (int i = 0; i < EMBEDDING_DIMENSIONS; i++) {
            if (i > 0) vectorStr.append(",");
            vectorStr.append(0.1f + (i * 0.001f));
        }
        vectorStr.append("]");
        return vectorStr.toString();
    }

    public float[] createDefaultEmbeddingFloatVector() {
        float[] mockEmbedding = new float[EMBEDDING_DIMENSIONS];
        for (int i = 0; i < mockEmbedding.length; i++) {
            mockEmbedding[i] = 0.1f + (i * 0.001f);
        }
        return mockEmbedding;
    }
}



