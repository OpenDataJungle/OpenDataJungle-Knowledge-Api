package com.opendatajungle.knowledge.api.infra.repository;

import com.opendatajungle.knowledge.api.business.exception.ResourceDeletionException;
import com.opendatajungle.knowledge.api.business.exception.SemanticSearchException;
import com.opendatajungle.knowledge.api.business.model.PartialResource;
import com.opendatajungle.knowledge.api.business.model.Resource;
import com.opendatajungle.knowledge.api.business.repository.VectorStoreRepository;
import com.opendatajungle.knowledge.api.business.service.splitter.DocumentSplitterFactory;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Repository
public class VectorRepositoryAdapter implements VectorStoreRepository {
    private static final Logger logger = LoggerFactory.getLogger(VectorRepositoryAdapter.class);

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingModel embeddingModel;
    private final DocumentSplitterFactory documentSplitterFactory;

    public VectorRepositoryAdapter(VectorStore vectorStore, JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel, DocumentSplitterFactory documentSplitterFactory) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingModel = embeddingModel;
        this.documentSplitterFactory = documentSplitterFactory;
    }

    public void addResource(Resource resource) {
        logger.info("Adding resource [{}] to vector store", resource.getName());

        List<String> chunks = documentSplitterFactory.getSplitter(resource).split(resource.getContent());

        List<Document> taggedDocs = chunks.stream()
                .map(chunk -> new Document(chunk, Map.of(
                        "resource_id", resource.getId().toString(),
                        "content_type", resource.getContentType(),
                        "status", resource.getStatus().name(),
                        "chunk_type", "content"
                )))
                .toList();

        vectorStore.add(taggedDocs);
        logger.info("[{}] Loaded {} documents", resource.getName(), taggedDocs.size());
    }

    public List<PartialResource> searchSimilar(String query, int limit, double minSimilarity, String currentUser, List<UUID> resourceIds) {
        try {
            float[] vector = embeddingModel.embed(query);

            PGobject pgVector = new PGobject();
            pgVector.setType("vector");
            StringBuilder vectorStr = new StringBuilder("[");
            for (int i = 0; i < vector.length; i++) {
                if (i > 0) vectorStr.append(",");
                vectorStr.append(vector[i]);
            }
            pgVector.setValue(vectorStr.append("]").toString());

            String resourceIdFilter = CollectionUtils.isEmpty(resourceIds) ? "" : " AND r.id = ANY(?)";

            String sql = """
                    WITH authorized_resources AS (
                        SELECT r.id, r.name, r.content_type, r.metadata, r.created_at, r.updated_at
                        FROM knowledge.resource r
                        LEFT JOIN knowledge.folder f ON r.folder_id = f.id
                        WHERE ( -- Security check: only return resources that the user has access to
                            r.created_by = ?
                            OR f.created_by = ?
                            OR f.id IN (
                                SELECT fg.folder_id FROM knowledge.folder_group fg
                                INNER JOIN reference_data.group_users gu ON fg.group_id = gu.group_id
                                INNER JOIN reference_data.users u ON gu.user_id = u.id AND u.username = ?
                                INNER JOIN reference_data.permissions p ON gu.permission_id = p.id AND p.can_read = true
                            )
                            OR r.id IN (
                                SELECT rgp.resource_id FROM knowledge.resource_group_permission rgp
                                INNER JOIN reference_data.group_users gu ON rgp.group_id = gu.group_id
                                INNER JOIN reference_data.users u ON gu.user_id = u.id AND u.username = ?
                                INNER JOIN reference_data.permissions p ON rgp.permission_id = p.id AND p.can_read = true
                            )
                        )
                    """ + resourceIdFilter + """
                    ),
                    search_results AS (
                        SELECT
                            v.id as vector_id,
                            v.content,
                            ar.id as resource_id,
                            ar.name as resource_name,
                            ar.content_type,
                            ar.metadata,
                            ar.created_at,
                            ar.updated_at,
                            (1 - (v.embedding <=> ?)) as similarity_score
                        FROM knowledge.vector_store v
                        INNER JOIN authorized_resources ar ON (v.metadata->>'resource_id')::uuid = ar.id
                        WHERE v.metadata->>'chunk_type' = 'content'
                    ),
                    deduped_results AS (
                        SELECT DISTINCT ON (content)
                            vector_id, content, resource_id, resource_name, content_type, metadata, created_at, updated_at, similarity_score
                        FROM search_results
                        ORDER BY content, created_at, vector_id DESC
                    )
                    SELECT vector_id, content, resource_id, resource_name, content_type, metadata, created_at, updated_at, similarity_score
                    FROM deduped_results
                    WHERE similarity_score >= ?
                    ORDER BY similarity_score DESC
                    LIMIT ?
                    """;

            return jdbcTemplate.query(
                    conn -> {
                        int paramIndex = 1;
                        PreparedStatement ps = conn.prepareStatement(sql);
                        ps.setObject(paramIndex++, currentUser);
                        ps.setObject(paramIndex++, currentUser);
                        ps.setObject(paramIndex++, currentUser);
                        ps.setObject(paramIndex++, currentUser);
                        if (StringUtils.hasText(resourceIdFilter)) {
                            ps.setArray(paramIndex++, conn.createArrayOf("uuid", resourceIds.toArray()));
                        }
                        ps.setObject(paramIndex++, pgVector);
                        ps.setDouble(paramIndex++, minSimilarity);
                        ps.setInt(paramIndex, limit);
                        return ps;
                    },
                    (rs, _) -> {
                        PartialResource partialResource = new PartialResource();
                        partialResource.setVectorId(UUID.fromString(rs.getString("vector_id")));
                        partialResource.setContent(rs.getString("content"));
                        partialResource.setResourceId(UUID.fromString(rs.getString("resource_id")));
                        partialResource.setResourceName(rs.getString("resource_name"));
                        partialResource.setContentType(rs.getString("content_type"));
                        partialResource.setMetadata(rs.getString("metadata"));
                        partialResource.setCreatedAt(rs.getTimestamp("created_at").toInstant());
                        partialResource.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
                        partialResource.setSimilarityScore(rs.getDouble("similarity_score"));
                        return partialResource;
                    }
            );

        } catch (Exception e) {
            logger.error("Error during semantic search", e);
            throw new SemanticSearchException(e);
        }
    }

    public void deleteResource(UUID resourceId) {
        try {
            String deleteSql = "DELETE FROM knowledge.vector_store WHERE metadata->>'resource_id' = ?";
            int deletedCount = jdbcTemplate.update(deleteSql, resourceId.toString());

            if (deletedCount > 0) {
                logger.info("Deleted {} documents for resource {}", deletedCount, resourceId);
            } else {
                logger.warn("No documents found to delete for resource {}", resourceId);
            }
        } catch (Exception e) {
            throw new ResourceDeletionException(resourceId, e);
        }
    }
}
