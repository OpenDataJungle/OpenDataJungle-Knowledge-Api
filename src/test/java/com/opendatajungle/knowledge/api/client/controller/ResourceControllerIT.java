package com.opendatajungle.knowledge.api.client.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opendatajungle.knowledge.api.business.model.ResourceStatus;
import com.opendatajungle.knowledge.api.business.service.ContentDownloaderUseCase;
import com.opendatajungle.knowledge.api.client.dto.CreateResourceRequest;
import com.opendatajungle.knowledge.api.client.dto.ResourceGroupPermissionRequest;
import com.opendatajungle.knowledge.api.client.dto.SearchRequest;
import com.opendatajungle.knowledge.api.infra.repository.ReferentialRepository;
import com.opendatajungle.knowledge.api.testconfig.TestDataLoader;
import com.opendatajungle.knowledge.api.testconfig.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
@Transactional
@Import(TestcontainersConfiguration.class)
class ResourceControllerIT {
    public static final String RESOURCES_PATH = "/api/v1/resources";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestDataLoader testDataLoader;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private EmbeddingModel embeddingModel;

    @MockitoBean
    private ContentDownloaderUseCase contentDownloaderUseCase;

    @MockitoBean
    private ReferentialRepository referentialRepository;

    @BeforeEach
    void setUp() {
        testDataLoader.cleanDatabase();
        testDataLoader.loadTestDefaultData();

        when(embeddingModel.embed(anyString())).thenReturn(testDataLoader.createDefaultEmbeddingFloatVector());
        // PgVectorStore#doAdd batches embeddings via this overload rather than embed(String)
        when(embeddingModel.embed(anyList(), any(), any()))
                .thenAnswer(invocation -> {
                    List<?> documents = invocation.getArgument(0);
                    return documents.stream()
                            .map(_ -> testDataLoader.createDefaultEmbeddingFloatVector())
                            .toList();
                });
    }

    @Test
    void getAllResources_shouldReturnAllResources() throws Exception {
        // When & Then
        mockMvc.perform(get(RESOURCES_PATH))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].id", notNullValue()))
                .andExpect(jsonPath("$[0].name", notNullValue()))
                .andExpect(jsonPath("$[0].content_type", notNullValue()))
                .andExpect(jsonPath("$[0].status", notNullValue()))
                .andExpect(jsonPath("$[0].created_at", notNullValue()))
                .andExpect(jsonPath("$[0].updated_at", notNullValue()));
    }

    @Test
    void getAllResources_shouldReturnEmptyList_whenNoResources() throws Exception {
        // Given
        testDataLoader.cleanDatabase();

        // When & Then
        mockMvc.perform(get(RESOURCES_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getResourceById_shouldReturnResource_whenExists() throws Exception {
        // Given
        String resourceId = "550e8400-e29b-41d4-a716-446655440000";

        // When & Then
        mockMvc.perform(get(RESOURCES_PATH + "/" + resourceId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(resourceId)))
                .andExpect(jsonPath("$.name", is("test")))
                .andExpect(jsonPath("$.content_type", is("text/plain")))
                .andExpect(jsonPath("$.status", notNullValue()));
    }

    @Test
    void getResourceById_shouldReturn404_whenNotExists() throws Exception {
        // Given
        String nonExistentId = "00000000-0000-0000-0000-000000000000";

        // When & Then
        mockMvc.perform(get(RESOURCES_PATH + "/" + nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchResourcesByName_shouldReturnMatchingResources() throws Exception {
        // When & Then
        mockMvc.perform(get(RESOURCES_PATH + "/search")
                        .param("name", "test"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].name", notNullValue()));
    }

    @Test
    void searchResourcesByName_shouldReturnEmptyList_whenNoMatch() throws Exception {
        // When & Then
        mockMvc.perform(get(RESOURCES_PATH + "/search")
                        .param("name", "nonexistent-resource-name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getResourcesByStatus_shouldReturnResourcesWithStatus() throws Exception {
        // When & Then
        mockMvc.perform(get(RESOURCES_PATH + "/status/" + ResourceStatus.VECTORIZED))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].status", is("VECTORIZED")));
    }

    @Test
    void getResourcesByStatus_shouldReturnEmptyList_whenNoResourcesWithStatus() throws Exception {
        // When & Then
        mockMvc.perform(get(RESOURCES_PATH + "/status/" + ResourceStatus.ERROR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void reprocessResource_shouldReVectorizeResource_whenResourceExists() throws Exception {
        // Given
        String resourceId = "550e8400-e29b-41d4-a716-446655440000";

        // When & Then
        mockMvc.perform(post(RESOURCES_PATH + "/" + resourceId + "/reprocess"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(resourceId)))
                .andExpect(jsonPath("$.status", is("VECTORIZED")));
    }

    @Test
    void reprocessResource_shouldReturn404_whenResourceNotExists() throws Exception {
        // Given
        String nonExistentId = "00000000-0000-0000-0000-000000000000";

        // When & Then
        mockMvc.perform(post(RESOURCES_PATH + "/" + nonExistentId + "/reprocess"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteResource_shouldDeleteExistingResource() throws Exception {
        // Given
        String resourceId = "550e8400-e29b-41d4-a716-446655440000";

        // When & Then
        mockMvc.perform(delete(RESOURCES_PATH + "/" + resourceId))
                .andExpect(status().isOk());

        // Verify resource is deleted
        mockMvc.perform(get(RESOURCES_PATH + "/" + resourceId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteResource_shouldReturn404_whenResourceNotExists() throws Exception {
        // Given
        String nonExistentId = "00000000-0000-0000-0000-000000000000";

        // When & Then
        mockMvc.perform(delete(RESOURCES_PATH + "/" + nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getResourceContent_shouldReturnContent_whenResourceExists() throws Exception {
        // Given
        String resourceId = "550e8400-e29b-41d4-a716-446655440000";

        // When & Then
        mockMvc.perform(get(RESOURCES_PATH + "/" + resourceId + "/content"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(resourceId)))
                .andExpect(jsonPath("$.name", is("test")))
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    void getResourceContent_shouldReturn404_whenResourceNotExists() throws Exception {
        // Given
        String nonExistentId = "00000000-0000-0000-0000-000000000000";

        // When & Then
        mockMvc.perform(get(RESOURCES_PATH + "/" + nonExistentId + "/content"))
                .andExpect(status().isNotFound());
    }

    @Test
    void renameResource_shouldUpdateName_whenResourceExists() throws Exception {
        // Given
        String resourceId = "550e8400-e29b-41d4-a716-446655440000";
        String newName = "updated-resource-name";

        // When & Then
        mockMvc.perform(patch(RESOURCES_PATH + "/" + resourceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + newName + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void renameResource_thenSemanticSearch_shouldReflectNewNameImmediately() throws Exception {
        // Given
        String resourceId = "550e8400-e29b-41d4-a716-446655440000";
        String newName = "renamed-before-search";

        // When renamed
        mockMvc.perform(patch(RESOURCES_PATH + "/" + resourceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + newName + "\"}"))
                .andExpect(status().isOk());

        // Then
        SearchRequest searchRequest = new SearchRequest("Java programming", 10, null, null);
        mockMvc.perform(post("/api/v1/search/semantic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.resource_id=='" + resourceId + "')].resource_name", hasItem(newName)));
    }

    @Test
    void renameResource_shouldReturn404_whenResourceNotExists() throws Exception {
        // Given
        String nonExistentId = "00000000-0000-0000-0000-000000000000";

        // When & Then
        mockMvc.perform(patch(RESOURCES_PATH + "/" + nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"new-name\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void renameResource_shouldReturn400_whenNameIsBlank() throws Exception {
        // Given
        String resourceId = "550e8400-e29b-41d4-a716-446655440000";

        // When & Then
        mockMvc.perform(patch(RESOURCES_PATH + "/" + resourceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createResource_shouldCreateTextResource_whenValidRequest() throws Exception {
        // Given
        CreateResourceRequest request = new CreateResourceRequest(
                "new-text-resource", "This is some text content to vectorize.", null,
                "TEXT", "{\"key\":\"value\"}", null, null);

        // When & Then
        mockMvc.perform(post(RESOURCES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("new-text-resource")))
                .andExpect(jsonPath("$.content_type", is("text/plain")))
                .andExpect(jsonPath("$.status", is("VECTORIZED")))
                .andExpect(jsonPath("$.source_type", is("TEXT")))
                .andExpect(jsonPath("$.folder_id", notNullValue()))
                .andExpect(jsonPath("$.created_by", is("anonymous")));

        // Verify resource is retrievable
        mockMvc.perform(get(RESOURCES_PATH + "/search").param("name", "new-text-resource"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void createResource_shouldUseDefaultSourceType_whenSourceTypeMissing() throws Exception {
        // Given
        CreateResourceRequest request = new CreateResourceRequest(
                "default-source-type-resource", "Some content.", null,
                null, null, null, null);

        // When & Then
        mockMvc.perform(post(RESOURCES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source_type", is("TEXT")));
    }

    @Test
    void createResource_shouldReturn400_whenNameIsBlank() throws Exception {
        // Given
        CreateResourceRequest request = new CreateResourceRequest(
                "", "Some content.", null, "TEXT", null, null, null);

        // When & Then
        mockMvc.perform(post(RESOURCES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("REQUIRED")))
                .andExpect(jsonPath("$.field", is("name")));
    }

    @Test
    void createResource_shouldReturn400_whenContentIsBlank() throws Exception {
        // Given
        CreateResourceRequest request = new CreateResourceRequest(
                "text-resource", "", null, "TEXT", null, null, null);

        // When & Then
        mockMvc.perform(post(RESOURCES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("REQUIRED")))
                .andExpect(jsonPath("$.field", is("content")));
    }

    @Test
    void createResource_shouldReturn400_whenSourceTypeUnsupported() throws Exception {
        // Given
        CreateResourceRequest request = new CreateResourceRequest(
                "unsupported-resource", "Some content.", null, "PDF", null, null, null);

        // When & Then
        mockMvc.perform(post(RESOURCES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("UNSUPPORTED_SOURCE_TYPE")));
    }

    @Test
    void createResource_shouldReturn404_whenFolderNotExists() throws Exception {
        // Given
        UUID nonExistentFolderId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        CreateResourceRequest request = new CreateResourceRequest(
                "resource-in-missing-folder", "Some content.", null, "TEXT", null, nonExistentFolderId, null);

        // When & Then
        mockMvc.perform(post(RESOURCES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createResource_shouldReturn400_whenGroupPermissionAccessDenied() throws Exception {
        // Given
        List<ResourceGroupPermissionRequest> groupPermissions = List.of(
                new ResourceGroupPermissionRequest(UUID.randomUUID(), UUID.randomUUID()));
        CreateResourceRequest request = new CreateResourceRequest(
                "resource-with-denied-group", "Some content.", null, "TEXT", null, null, groupPermissions);

        // When & Then
        mockMvc.perform(post(RESOURCES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("RESOURCE_GROUP_ACCESS_DENIED")));
    }

    @Test
    void createResource_shouldCreateUrlResource_whenValidRequest() throws Exception {
        // Given
        when(contentDownloaderUseCase.downloadContent(anyString()))
                .thenReturn("Downloaded content for the URL resource.");

        CreateResourceRequest request = new CreateResourceRequest(
                "url-resource", null, "https://laulem.com/doc", "URL", null, null, null);

        // When & Then
        mockMvc.perform(post(RESOURCES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source_type", is("URL")))
                .andExpect(jsonPath("$.source_name", is("https://laulem.com/doc")))
                .andExpect(jsonPath("$.status", is("VECTORIZED")));
    }

    @Test
    void createResource_shouldReturn400_whenUrlIsBlank() throws Exception {
        // Given
        CreateResourceRequest request = new CreateResourceRequest(
                "url-resource", null, "", "URL", null, null, null);

        // When & Then
        mockMvc.perform(post(RESOURCES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("REQUIRED")))
                .andExpect(jsonPath("$.field", is("url")));
    }

    @Test
    void createResourceFromFile_shouldCreateResource_whenValidTxtFile() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", MediaType.TEXT_PLAIN_VALUE,
                "File content for vectorization.".getBytes(StandardCharsets.UTF_8));

        // When & Then
        mockMvc.perform(multipart(RESOURCES_PATH + "/upload")
                        .file(file)
                        .param("name", "notes-resource"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("notes-resource")))
                .andExpect(jsonPath("$.source_type", is("FILE")))
                .andExpect(jsonPath("$.source_name", is("notes.txt")))
                .andExpect(jsonPath("$.status", is("VECTORIZED")));
    }

    @Test
    void createResourceFromFile_shouldReturn400_whenFileIsEmpty() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.txt", MediaType.TEXT_PLAIN_VALUE, new byte[0]);

        // When & Then
        mockMvc.perform(multipart(RESOURCES_PATH + "/upload")
                        .file(file)
                        .param("name", "empty-resource"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field", is("file")));
    }

    @Test
    void createResourceFromFile_shouldReturn400_whenExtensionUnsupported() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", MediaType.APPLICATION_PDF_VALUE, "content".getBytes(StandardCharsets.UTF_8));

        // When & Then
        mockMvc.perform(multipart(RESOURCES_PATH + "/upload")
                        .file(file)
                        .param("name", "pdf-resource"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("UNSUPPORTED_FILE_EXTENSION")));
    }

    @Test
    void createResource_shouldUseSpecificFolder_whenFolderIdProvided() throws Exception {
        // Given
        UUID rootFolderId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        CreateResourceRequest request = new CreateResourceRequest(
                "resource-in-root-folder", "Some content.", null, "TEXT", null, rootFolderId, null);

        // When & Then
        mockMvc.perform(post(RESOURCES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.folder_id", is(rootFolderId.toString())));
    }

    @Test
    void createResource_shouldCreateResource_whenGroupPermissionGranted() throws Exception {
        // Given: the "root" group/permission seeded by init-referential.sql, so the
        // resource_group_permission FK constraints resolve to real rows.
        when(referentialRepository.hasGroupWriteAccess(any(), any())).thenReturn(true);
        UUID groupId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID permissionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        List<ResourceGroupPermissionRequest> groupPermissions = List.of(
                new ResourceGroupPermissionRequest(groupId, permissionId));
        CreateResourceRequest request = new CreateResourceRequest(
                "resource-with-granted-group", "Some content.", null, "TEXT", null, null, groupPermissions);

        // When & Then
        mockMvc.perform(post(RESOURCES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.group_permissions", hasSize(1)))
                .andExpect(jsonPath("$.group_permissions[0].group_id", is(groupId.toString())))
                .andExpect(jsonPath("$.group_permissions[0].permission_id", is(permissionId.toString())));
    }

    @Test
    void createResource_thenSemanticSearch_shouldFindNewlyVectorizedContent() throws Exception {
        // Given
        CreateResourceRequest request = new CreateResourceRequest(
                "search-roundtrip-resource", "Unique searchable content about quantum computing.", null,
                "TEXT", null, null, null);
        String createResponse = mockMvc.perform(post(RESOURCES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String resourceId = objectMapper.readTree(createResponse).get("id").asText();

        SearchRequest searchRequest = new SearchRequest("quantum computing", 10, null, null);

        // When & Then
        mockMvc.perform(post("/api/v1/search/semantic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.resource_id=='" + resourceId + "')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.resource_id=='" + resourceId + "')].content",
                        hasItem("Unique searchable content about quantum computing.")));
    }

    @Test
    void semanticSearch_shouldRestrictResults_whenResourceIdsProvided() throws Exception {
        // Given: two distinct resources sharing the same searchable phrase
        CreateResourceRequest requestA = new CreateResourceRequest(
                "resource-a", "Shared topic content about deep sea exploration.", null, "TEXT", null, null, null);
        String responseA = mockMvc.perform(post(RESOURCES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestA)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String resourceIdA = objectMapper.readTree(responseA).get("id").asText();

        CreateResourceRequest requestB = new CreateResourceRequest(
                "resource-b", "Shared topic content about deep sea exploration.", null, "TEXT", null, null, null);
        String responseB = mockMvc.perform(post(RESOURCES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestB)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String resourceIdB = objectMapper.readTree(responseB).get("id").asText();

        // When: searching restricted to resource A's id only
        SearchRequest searchRequest = new SearchRequest(
                "deep sea exploration", 10, null, List.of(UUID.fromString(resourceIdA)));

        // Then: only resource A appears, resource B is filtered out despite matching equally well
        mockMvc.perform(post("/api/v1/search/semantic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.resource_id=='" + resourceIdA + "')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.resource_id=='" + resourceIdB + "')]", hasSize(0)));
    }

    @Test
    void createResourceFromFile_thenSemanticSearch_shouldFindNewlyVectorizedContent() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "quantum.txt", MediaType.TEXT_PLAIN_VALUE,
                "Distinctive file content about black holes and gravity.".getBytes(StandardCharsets.UTF_8));
        String createResponse = mockMvc.perform(multipart(RESOURCES_PATH + "/upload")
                        .file(file)
                        .param("name", "black-holes-resource"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String resourceId = objectMapper.readTree(createResponse).get("id").asText();

        SearchRequest searchRequest = new SearchRequest("black holes and gravity", 10, null, null);

        // When & Then
        mockMvc.perform(post("/api/v1/search/semantic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.resource_id=='" + resourceId + "')]", hasSize(1)));
    }

    @Test
    void deleteResource_shouldRemoveVectors_whenResourceIsDeleted() throws Exception {
        // Given
        String resourceId = "550e8400-e29b-41d4-a716-446655440000";

        // When
        mockMvc.perform(delete(RESOURCES_PATH + "/" + resourceId))
                .andExpect(status().isOk());

        // Then: the resource's vectors must no longer be returned by semantic search
        SearchRequest searchRequest = new SearchRequest("Java programming", 10, null, null);
        mockMvc.perform(post("/api/v1/search/semantic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.resource_id=='" + resourceId + "')]", hasSize(0)));
    }
}

