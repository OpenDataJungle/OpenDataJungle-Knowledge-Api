package com.laulem.vectopath.knowledge.api.client.controller;

import com.laulem.vectopath.knowledge.api.business.model.ResourceStatus;
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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// TODO : Add tests for createResource and createResourceFromFile
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
@Transactional
@Import(TestcontainersConfiguration.class)
class ResourceControllerIntegrationTest {
    public static final String RESOURCES_PATH = "/api/v1/resources";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestDataLoader testDataLoader;

    @MockitoBean
    private EmbeddingModel embeddingModel;

    @BeforeEach
    void setUp() {
        testDataLoader.cleanDatabase();
        testDataLoader.loadTestDefaultData();

        when(embeddingModel.embed(anyString())).thenReturn(testDataLoader.createDefaultEmbeddingFloatVector());
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
    void deleteResource_shouldNotFail_whenResourceNotExists() throws Exception {
        // Given
        String nonExistentId = "00000000-0000-0000-0000-000000000000";

        // When & Then
        mockMvc.perform(delete(RESOURCES_PATH + "/" + nonExistentId))
                .andExpect(status().isOk());
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
}

