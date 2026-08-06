package com.laulem.vectopath.knowledge.api.client.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laulem.vectopath.knowledge.api.client.dto.CreateResourceRequest;
import com.laulem.vectopath.knowledge.api.client.dto.FolderRequest;
import com.laulem.vectopath.knowledge.api.client.dto.SearchRequest;
import com.laulem.vectopath.knowledge.api.infra.repository.ReferentialRepository;
import com.laulem.vectopath.knowledge.api.testconfig.TestDataLoader;
import com.laulem.vectopath.knowledge.api.testconfig.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
@Transactional
@Import(TestcontainersConfiguration.class)
class FolderControllerIT {
    public static final String FOLDERS_PATH = "/api/v1/folders";
    public static final String RESOURCES_PATH = "/api/v1/resources";
    public static final String ROOT_FOLDER_ID = "00000000-0000-0000-0000-000000000001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestDataLoader testDataLoader;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ReferentialRepository referentialRepository;

    @MockitoBean
    private EmbeddingModel embeddingModel;

    @BeforeEach
    void setUp() {
        testDataLoader.cleanDatabase();

        when(embeddingModel.embed(anyString())).thenReturn(testDataLoader.createDefaultEmbeddingFloatVector());
        when(embeddingModel.embed(anyList(), any(), any()))
                .thenAnswer(invocation -> {
                    List<?> documents = invocation.getArgument(0);
                    return documents.stream()
                            .map(_ -> testDataLoader.createDefaultEmbeddingFloatVector())
                            .toList();
                });
    }

    @Test
    void create_shouldCreateFolder_whenValidRequest() throws Exception {
        // Given
        FolderRequest request = new FolderRequest("subfolder", "ROOT", null);

        // When & Then
        mockMvc.perform(post(FOLDERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("subfolder")))
                .andExpect(jsonPath("$.path", is("ROOT")))
                .andExpect(jsonPath("$.completePath", is("ROOT/subfolder")))
                .andExpect(jsonPath("$.parentId", is(ROOT_FOLDER_ID)))
                .andExpect(jsonPath("$.createdBy", is("anonymous")));
    }

    @Test
    void create_shouldReturn400_whenNameIsBlank() throws Exception {
        // Given
        FolderRequest request = new FolderRequest("", "ROOT", null);

        // When & Then
        mockMvc.perform(post(FOLDERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("FOLDER_PATH_OR_NAME_NULL")));
    }

    @Test
    void create_shouldReturn400_whenParentPathNotFound() throws Exception {
        // Given
        FolderRequest request = new FolderRequest("subfolder", "ROOT/missing-parent", null);

        // When & Then
        mockMvc.perform(post(FOLDERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("FOLDER_PARENT_NOT_FOUND")));
    }

    @Test
    void create_shouldReturn400_whenPathAlreadyExists() throws Exception {
        // Given
        FolderRequest request = new FolderRequest("subfolder", "ROOT", null);
        mockMvc.perform(post(FOLDERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // When & Then
        mockMvc.perform(post(FOLDERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("FOLDER_PATH_EXISTS")));
    }

    @Test
    void create_shouldReturn400_whenGroupAccessDenied() throws Exception {
        // Given
        FolderRequest request = new FolderRequest("subfolder", "ROOT", List.of(UUID.randomUUID()));

        // When & Then
        mockMvc.perform(post(FOLDERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("FOLDER_GROUP_ACCESS_DENIED")));
    }

    @Test
    void create_shouldCreateFolder_whenGroupAccessGranted() throws Exception {
        // Given: the "root" group seeded by init-referential.sql, so the folder_group FK
        // constraint resolves to a real row.
        when(referentialRepository.hasGroupWriteAccess(any(), any())).thenReturn(true);
        UUID groupId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        FolderRequest request = new FolderRequest("subfolder", "ROOT", List.of(groupId));

        // When & Then
        mockMvc.perform(post(FOLDERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupIds", hasItem(groupId.toString())));
    }

    @Test
    void listAll_shouldReturnFolders() throws Exception {
        // When & Then
        mockMvc.perform(get(FOLDERS_PATH))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].id", notNullValue()));
    }

    @Test
    void getMyDefaultFolder_shouldCreateAndReturnUserFolder() throws Exception {
        // When & Then
        mockMvc.perform(post(FOLDERS_PATH + "/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("anonymous")))
                .andExpect(jsonPath("$.completePath", is("ROOT/USERS/anonymous")));
    }

    @Test
    void getMyDefaultFolder_shouldReturnSameFolder_whenCalledTwice() throws Exception {
        // Given
        String firstResponse = mockMvc.perform(post(FOLDERS_PATH + "/me"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // When
        String secondResponse = mockMvc.perform(post(FOLDERS_PATH + "/me"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Then
        String firstFolderId = objectMapper.readTree(firstResponse).get("id").asText();
        String secondFolderId = objectMapper.readTree(secondResponse).get("id").asText();
        assertThat(secondFolderId).isEqualTo(firstFolderId);
    }

    @Test
    void getById_shouldReturnFolder_whenExists() throws Exception {
        // When & Then
        mockMvc.perform(get(FOLDERS_PATH + "/" + ROOT_FOLDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(ROOT_FOLDER_ID)))
                .andExpect(jsonPath("$.name", is("Root Folder")))
                .andExpect(jsonPath("$.path", is("ROOT")));
    }

    @Test
    void getById_shouldReturn404_whenNotExists() throws Exception {
        // Given
        String nonExistentId = UUID.randomUUID().toString();

        // When & Then
        mockMvc.perform(get(FOLDERS_PATH + "/" + nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void findAllChildren_shouldReturnDirectChildren() throws Exception {
        // Given
        FolderRequest childRequest = new FolderRequest("child", "ROOT", null);
        mockMvc.perform(post(FOLDERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childRequest)))
                .andExpect(status().isCreated());

        // When & Then
        mockMvc.perform(get(FOLDERS_PATH + "/" + ROOT_FOLDER_ID + "/children"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("child")));
    }

    @Test
    void findAllChildren_shouldReturnEmptyList_whenNoChildren() throws Exception {
        // When & Then
        mockMvc.perform(get(FOLDERS_PATH + "/" + ROOT_FOLDER_ID + "/children"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void findAllChildren_shouldReturn404_whenParentNotExists() throws Exception {
        // Given
        String nonExistentId = UUID.randomUUID().toString();

        // When & Then
        mockMvc.perform(get(FOLDERS_PATH + "/" + nonExistentId + "/children"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldUpdateFolder_whenValidRequest() throws Exception {
        // Given
        FolderRequest createRequest = new FolderRequest("subfolder", "ROOT", null);
        String response = mockMvc.perform(post(FOLDERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String folderId = objectMapper.readTree(response).get("id").asText();

        FolderRequest updateRequest = new FolderRequest("renamed-subfolder", "ROOT", null);

        // When & Then
        mockMvc.perform(put(FOLDERS_PATH + "/" + folderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("renamed-subfolder")))
                .andExpect(jsonPath("$.completePath", is("ROOT/renamed-subfolder")));
    }

    @Test
    void update_shouldReturn400_whenNameIsBlank() throws Exception {
        // Given
        FolderRequest updateRequest = new FolderRequest("", "ROOT", null);

        // When & Then
        mockMvc.perform(put(FOLDERS_PATH + "/" + ROOT_FOLDER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("FOLDER_PATH_OR_NAME_NULL")));
    }

    @Test
    void update_shouldReturn404_whenFolderNotExists() throws Exception {
        // Given
        String nonExistentId = UUID.randomUUID().toString();
        FolderRequest updateRequest = new FolderRequest("new-name", "ROOT", null);

        // When & Then
        mockMvc.perform(put(FOLDERS_PATH + "/" + nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldReturn400_whenPathAlreadyExists() throws Exception {
        // Given
        FolderRequest firstRequest = new FolderRequest("first-folder", "ROOT", null);
        mockMvc.perform(post(FOLDERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        FolderRequest secondRequest = new FolderRequest("second-folder", "ROOT", null);
        String response = mockMvc.perform(post(FOLDERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String secondFolderId = objectMapper.readTree(response).get("id").asText();

        FolderRequest conflictingUpdate = new FolderRequest("first-folder", "ROOT", null);

        // When & Then
        mockMvc.perform(put(FOLDERS_PATH + "/" + secondFolderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflictingUpdate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("FOLDER_PATH_EXISTS")));
    }

    @Test
    void delete_shouldDeleteFolder_whenExists() throws Exception {
        // Given
        FolderRequest createRequest = new FolderRequest("to-delete", "ROOT", null);
        String response = mockMvc.perform(post(FOLDERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String folderId = objectMapper.readTree(response).get("id").asText();

        // When & Then
        mockMvc.perform(delete(FOLDERS_PATH + "/" + folderId))
                .andExpect(status().isOk());

        mockMvc.perform(get(FOLDERS_PATH + "/" + folderId))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldReturn404_whenNotExists() throws Exception {
        // Given
        String nonExistentId = UUID.randomUUID().toString();

        // When & Then
        mockMvc.perform(delete(FOLDERS_PATH + "/" + nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldCascadeDeleteResources_whenFolderContainsResources() throws Exception {
        // Given: a folder containing a vectorized resource
        FolderRequest folderRequest = new FolderRequest("folder-with-resource", "ROOT", null);
        String folderResponse = mockMvc.perform(post(FOLDERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(folderRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String folderId = objectMapper.readTree(folderResponse).get("id").asText();

        CreateResourceRequest resourceRequest = new CreateResourceRequest(
                "resource-in-folder", "Some content.", null, "TEXT", null, UUID.fromString(folderId), null);
        String resourceResponse = mockMvc.perform(post(RESOURCES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resourceRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String resourceId = objectMapper.readTree(resourceResponse).get("id").asText();

        // When: the folder is deleted
        mockMvc.perform(delete(FOLDERS_PATH + "/" + folderId))
                .andExpect(status().isOk());

        // Then: the resource inside it is gone too (DB-level ON DELETE CASCADE on folder_id)
        mockMvc.perform(get(RESOURCES_PATH + "/" + resourceId))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_thenSemanticSearch_shouldNotReturnCascadedResource() throws Exception {
        // Given: a folder containing a vectorized resource
        FolderRequest folderRequest = new FolderRequest("folder-with-searchable-resource", "ROOT", null);
        String folderResponse = mockMvc.perform(post(FOLDERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(folderRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String folderId = objectMapper.readTree(folderResponse).get("id").asText();

        CreateResourceRequest resourceRequest = new CreateResourceRequest(
                "resource-to-be-cascaded", "Unique cascaded content about volcanoes.", null, "TEXT", null,
                UUID.fromString(folderId), null);
        String resourceResponse = mockMvc.perform(post(RESOURCES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resourceRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String resourceId = objectMapper.readTree(resourceResponse).get("id").asText();

        // When: the folder is deleted, then immediately searched within the same request cycle
        mockMvc.perform(delete(FOLDERS_PATH + "/" + folderId))
                .andExpect(status().isOk());

        // Then: semantic search — a raw-JDBC read, unlike GET /resources/{id} — must no longer see
        // the cascaded resource's vectors, not a stale row left over from before the cascade fired
        SearchRequest searchRequest = new SearchRequest("volcanoes", 10, null, null);
        mockMvc.perform(post("/api/v1/search/semantic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.resource_id=='" + resourceId + "')]", hasSize(0)));
    }
}
