package com.opendatajungle.knowledge.api.infra.repository;

import com.opendatajungle.knowledge.api.business.exception.ResourceDeletionException;
import com.opendatajungle.knowledge.api.business.exception.SemanticSearchException;
import com.opendatajungle.knowledge.api.business.model.PartialResource;
import com.opendatajungle.knowledge.api.business.model.Resource;
import com.opendatajungle.knowledge.api.business.model.ResourceStatus;
import com.opendatajungle.knowledge.api.business.service.splitter.DocumentSplitter;
import com.opendatajungle.knowledge.api.business.service.splitter.DocumentSplitterFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PGobject;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VectorRepositoryAdapterTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private DocumentSplitterFactory documentSplitterFactory;

    @Mock
    private DocumentSplitter documentSplitter;

    @InjectMocks
    private VectorRepositoryAdapter adapter;

    @Test
    void addResource_shouldSplitContentAndAddTaggedDocumentsToVectorStore() {
        // Given
        Resource resource = new Resource("doc", "chunk one. chunk two.", "text/plain", null);
        resource.setId(UUID.randomUUID());
        resource.setStatus(ResourceStatus.PROCESSING);
        when(documentSplitterFactory.getSplitter(resource)).thenReturn(documentSplitter);
        when(documentSplitter.split(resource.getContent())).thenReturn(List.of("chunk one.", "chunk two."));

        // When
        adapter.addResource(resource);

        // Then
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        List<Document> documents = captor.getValue();
        assertThat(documents).hasSize(2);
        assertThat(documents.get(0).getText()).isEqualTo("chunk one.");
        assertThat(documents.get(0).getMetadata())
                .containsEntry("resource_id", resource.getId().toString())
                .containsEntry("chunk_type", "content");
    }

    @Test
    void searchSimilar_shouldWrapEmbeddingFailureInSemanticSearchException() {
        // Given
        when(embeddingModel.embed(anyString())).thenThrow(new RuntimeException("embedding service down"));

        // When & Then
        assertThatThrownBy(() -> adapter.searchSimilar("query", 5, 0.5, "alice", null))
                .isInstanceOf(SemanticSearchException.class);
    }

    @Test
    void searchSimilar_shouldWrapQueryFailureInSemanticSearchException() {
        // Given
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        when(jdbcTemplate.query(any(PreparedStatementCreator.class), any(RowMapper.class)))
                .thenThrow(new RuntimeException("db down"));

        // When & Then
        assertThatThrownBy(() -> adapter.searchSimilar("query", 5, 0.5, "alice", null))
                .isInstanceOf(SemanticSearchException.class);
    }

    @Test
    void searchSimilar_shouldBindParametersInOrder_withoutResourceIdFilter() throws Exception {
        // Given
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        when(jdbcTemplate.query(any(PreparedStatementCreator.class), any(RowMapper.class))).thenReturn(List.of());

        // When
        adapter.searchSimilar("query", 5, 0.5, "alice", null);

        // Then
        ArgumentCaptor<PreparedStatementCreator> pscCaptor = ArgumentCaptor.forClass(PreparedStatementCreator.class);
        verify(jdbcTemplate).query(pscCaptor.capture(), any(RowMapper.class));

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        pscCaptor.getValue().createPreparedStatement(connection);

        verify(preparedStatement).setObject(1, "alice");
        verify(preparedStatement).setObject(2, "alice");
        verify(preparedStatement).setObject(3, "alice");
        verify(preparedStatement).setObject(4, "alice");
        verify(preparedStatement, never()).setArray(anyInt(), any());
        verify(preparedStatement).setObject(eq(5), any(PGobject.class));
        verify(preparedStatement).setDouble(6, 0.5);
        verify(preparedStatement).setInt(7, 5);
        verify(preparedStatement, times(5)).setObject(anyInt(), any());
    }

    @Test
    void searchSimilar_shouldBindResourceIdArray_whenResourceIdsProvided() throws Exception {
        // Given
        UUID resourceId = UUID.randomUUID();
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        when(jdbcTemplate.query(any(PreparedStatementCreator.class), any(RowMapper.class))).thenReturn(List.of());

        // When
        adapter.searchSimilar("query", 5, 0.5, "alice", List.of(resourceId));

        // Then
        ArgumentCaptor<PreparedStatementCreator> pscCaptor = ArgumentCaptor.forClass(PreparedStatementCreator.class);
        verify(jdbcTemplate).query(pscCaptor.capture(), any(RowMapper.class));

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        Array sqlArray = mock(Array.class);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(connection.createArrayOf(eq("uuid"), any())).thenReturn(sqlArray);

        pscCaptor.getValue().createPreparedStatement(connection);

        verify(preparedStatement).setObject(1, "alice");
        verify(preparedStatement).setObject(2, "alice");
        verify(preparedStatement).setObject(3, "alice");
        verify(preparedStatement).setObject(4, "alice");
        verify(preparedStatement).setArray(5, sqlArray);
        verify(preparedStatement).setObject(eq(6), any(PGobject.class));
        verify(preparedStatement).setDouble(7, 0.5);
        verify(preparedStatement).setInt(8, 5);
        verify(preparedStatement, times(5)).setObject(anyInt(), any());
    }

    @Test
    void searchSimilar_shouldMapResultSetRowsToPartialResource() throws Exception {
        // Given
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        UUID vectorId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("vector_id")).thenReturn(vectorId.toString());
        when(resultSet.getString("content")).thenReturn("some content");
        when(resultSet.getString("resource_id")).thenReturn(resourceId.toString());
        when(resultSet.getString("resource_name")).thenReturn("doc");
        when(resultSet.getString("content_type")).thenReturn("text/plain");
        when(resultSet.getString("metadata")).thenReturn("{\"page\":1}");
        LocalDateTime createdAt = now.minusDays(1);
        when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(createdAt));
        when(resultSet.getTimestamp("updated_at")).thenReturn(Timestamp.valueOf(now));
        when(resultSet.getDouble("similarity_score")).thenReturn(0.87);

        when(jdbcTemplate.query(any(PreparedStatementCreator.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<PartialResource> rowMapper = invocation.getArgument(1);
                    return List.of(rowMapper.mapRow(resultSet, 0));
                });

        // When
        List<PartialResource> results = adapter.searchSimilar("query", 5, 0.5, "alice", null);

        // Then
        assertThat(results).hasSize(1);
        PartialResource result = results.getFirst();
        assertThat(result.getVectorId()).isEqualTo(vectorId);
        assertThat(result.getResourceId()).isEqualTo(resourceId);
        assertThat(result.getResourceName()).isEqualTo("doc");
        assertThat(result.getContent()).isEqualTo("some content");
        assertThat(result.getContentType()).isEqualTo("text/plain");
        assertThat(result.getMetadata()).isEqualTo("{\"page\":1}");
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        assertThat(result.getUpdatedAt()).isEqualTo(now);
        assertThat(result.getSimilarityScore()).isEqualTo(0.87);
    }

    @Test
    void deleteResource_shouldDeleteVectorStoreRowsForResource() {
        // Given
        UUID resourceId = UUID.randomUUID();
        when(jdbcTemplate.update(anyString(), eq(resourceId.toString()))).thenReturn(3);

        // When
        adapter.deleteResource(resourceId);

        // Then
        verify(jdbcTemplate).update(anyString(), eq(resourceId.toString()));
    }

    @Test
    void deleteResource_shouldWrapFailureInResourceDeletionException() {
        // Given
        UUID resourceId = UUID.randomUUID();
        when(jdbcTemplate.update(anyString(), anyString())).thenThrow(new RuntimeException("db down"));

        // When & Then
        assertThatThrownBy(() -> adapter.deleteResource(resourceId))
                .isInstanceOf(ResourceDeletionException.class);
    }
}
