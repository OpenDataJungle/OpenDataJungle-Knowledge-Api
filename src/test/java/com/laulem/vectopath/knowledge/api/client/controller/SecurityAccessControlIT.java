package com.laulem.vectopath.knowledge.api.client.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laulem.vectopath.knowledge.api.client.dto.CreateResourceRequest;
import com.laulem.vectopath.knowledge.api.client.dto.FolderRequest;
import com.laulem.vectopath.knowledge.api.client.dto.ResourceGroupPermissionRequest;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs under the "security-it" profile (neither "test" nor "local"), which activates the real
 * {@code SecurityConfiguration} (real {@code @EnableMethodSecurity}, real {@code @PreAuthorize}
 * scope checks) instead of the {@code WithoutSecurityConfiguration} bypass used by every other
 * *IT test. {@link JwtDecoder} is mocked purely to stop Spring Boot from resolving a real OIDC
 * issuer at context startup; {@link SecurityMockMvcRequestPostProcessors#jwt()} injects a fully
 * formed principal per request without ever calling that decoder, so no signature/Keycloak is
 * involved while {@code @PreAuthorize} is genuinely evaluated.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("security-it")
@Transactional
@Import(TestcontainersConfiguration.class)
class SecurityAccessControlIT {
    private static final String RESOURCES_PATH = "/api/v1/resources";
    private static final String FOLDERS_PATH = "/api/v1/folders";
    private static final UUID READ_GROUP_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestDataLoader testDataLoader;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private EmbeddingModel embeddingModel;

    @MockitoBean
    private ReferentialRepository referentialRepository;

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

        // "alice" owns nothing by default (unlike the seeded "anonymous" user, who owns the seeded
        // ROOT folder), so she needs write access granted through the root group to create anything
        // at all under ROOT in these tests. Group assignment is left permissive for her too, since
        // group-assignment authorization itself is already covered by FolderControllerIT.
        when(referentialRepository.getGroupWriteAccess(eq("alice")))
                .thenReturn(List.of(UUID.fromString("00000000-0000-0000-0000-000000000001")));
        when(referentialRepository.hasGroupWriteAccess(any(), eq("alice"))).thenReturn(true);

        // Second user "bob", distinct from the seeded default "anonymous", for cross-user isolation tests
        jdbcTemplate.update("""
                INSERT INTO referential.users (id, first_name, last_name, username)
                VALUES ('00000000-0000-0000-0000-000000000002', 'Bob', 'User', 'bob')
                ON CONFLICT DO NOTHING
                """);
    }

    private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder builder, String username, String... scopes) {
        SimpleGrantedAuthority[] authorities = List.of(scopes).stream()
                .map(SimpleGrantedAuthority::new)
                .toArray(SimpleGrantedAuthority[]::new);
        return builder.with(jwt()
                .jwt(token -> token.claim("preferred_username", username))
                .authorities(authorities));
    }

    // ---- Scope enforcement (@PreAuthorize) ----

    @Test
    void getAllResources_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get(RESOURCES_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllResources_shouldReturn403_whenMissingReadScope() throws Exception {
        mockMvc.perform(asUser(get(RESOURCES_PATH), "anonymous", "folders.read"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllResources_shouldReturn200_whenReadScopePresent() throws Exception {
        mockMvc.perform(asUser(get(RESOURCES_PATH), "anonymous", "resources.read"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteResource_shouldReturn403_whenMissingDeleteScope() throws Exception {
        String resourceId = "550e8400-e29b-41d4-a716-446655440000";
        testDataLoader.loadTestDefaultData();

        mockMvc.perform(asUser(delete(RESOURCES_PATH + "/" + resourceId), "anonymous", "resources.read", "resources.write"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createFolder_shouldReturn403_whenMissingWriteScope() throws Exception {
        FolderRequest request = new FolderRequest("subfolder", "ROOT", null);

        mockMvc.perform(asUser(post(FOLDERS_PATH), "anonymous", "folders.read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createFolder_shouldReturn201_whenWriteScopePresent() throws Exception {
        FolderRequest request = new FolderRequest("subfolder", "ROOT", null);

        mockMvc.perform(asUser(post(FOLDERS_PATH), "anonymous", "folders.write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void semanticSearch_shouldReturn403_whenMissingSearchScope() throws Exception {
        String body = "{\"query\":\"test\"}";

        mockMvc.perform(asUser(post("/api/v1/search/semantic"), "anonymous", "resources.read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    // ---- Cross-user data isolation ----

    @Test
    void bob_shouldNotSeeAlicesPrivateFolder() throws Exception {
        // Given: alice creates a folder she owns exclusively
        String response = mockMvc.perform(asUser(post(FOLDERS_PATH), "alice", "folders.write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FolderRequest("alice-private", "ROOT", null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String folderId = objectMapper.readTree(response).get("id").asText();

        // When & Then: bob cannot read, update, or delete it — masked as 404, not 403
        mockMvc.perform(asUser(get(FOLDERS_PATH + "/" + folderId), "bob", "folders.read"))
                .andExpect(status().isNotFound());

        mockMvc.perform(asUser(put(FOLDERS_PATH + "/" + folderId), "bob", "folders.write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FolderRequest("hijacked", "ROOT", null))))
                .andExpect(status().isNotFound());

        mockMvc.perform(asUser(delete(FOLDERS_PATH + "/" + folderId), "bob", "folders.delete"))
                .andExpect(status().isNotFound());
    }

    @Test
    void bob_shouldNotSeeAlicesPrivateResource() throws Exception {
        // Given: alice creates a resource she owns exclusively
        CreateResourceRequest createRequest = new CreateResourceRequest(
                "alice-private-resource", "Content only alice should see.", null, "TEXT", null, null, null);
        String response = mockMvc.perform(asUser(post(RESOURCES_PATH), "alice", "resources.write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String resourceId = objectMapper.readTree(response).get("id").asText();

        // When & Then: bob cannot read, rename, or delete it
        mockMvc.perform(asUser(get(RESOURCES_PATH + "/" + resourceId), "bob", "resources.read"))
                .andExpect(status().isNotFound());

        mockMvc.perform(asUser(patch(RESOURCES_PATH + "/" + resourceId), "bob", "resources.write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"hijacked\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(asUser(delete(RESOURCES_PATH + "/" + resourceId), "bob", "resources.delete"))
                .andExpect(status().isNotFound());

        // And bob's own resource listing must not include it either
        mockMvc.perform(asUser(get(RESOURCES_PATH), "bob", "resources.read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + resourceId + "')]", hasSize(0)));
    }

    @Test
    void bob_shouldReadAlicesFolder_whenSharedViaGroupMembership() throws Exception {
        // Given: a read-only group that bob belongs to
        seedReadOnlyGroupMembership("bob", READ_GROUP_ID);

        // alice creates a folder and shares it with that group
        String response = mockMvc.perform(asUser(post(FOLDERS_PATH), "alice", "folders.write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FolderRequest("shared-with-bob", "ROOT", List.of(READ_GROUP_ID)))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String folderId = objectMapper.readTree(response).get("id").asText();

        // When & Then: bob can read it via the shared group, even though he doesn't own it
        mockMvc.perform(asUser(get(FOLDERS_PATH + "/" + folderId), "bob", "folders.read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(folderId)));

        // But read-only membership must not grant write access
        mockMvc.perform(asUser(delete(FOLDERS_PATH + "/" + folderId), "bob", "folders.delete"))
                .andExpect(status().isNotFound());
    }

    @Test
    void bob_shouldWriteAlicesFolder_whenGrantedGroupWriteAccessExternally() throws Exception {
        // Given: alice creates a folder shared with a group
        seedReadOnlyGroupMembership("bob", READ_GROUP_ID);
        String response = mockMvc.perform(asUser(post(FOLDERS_PATH), "alice", "folders.write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FolderRequest("shared-writable", "ROOT", List.of(READ_GROUP_ID)))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String folderId = objectMapper.readTree(response).get("id").asText();

        // The external referential service is the source of truth for write access;
        // simulate it granting bob write access through that same group.
        when(referentialRepository.getGroupWriteAccess(eq("bob"))).thenReturn(List.of(READ_GROUP_ID));

        // When & Then: bob, who only has local read-membership, can now also delete it
        mockMvc.perform(asUser(delete(FOLDERS_PATH + "/" + folderId), "bob", "folders.delete"))
                .andExpect(status().isOk());
    }

    // ---- Direct resource-level group sharing (knowledge.resource_group_permission) ----

    @Test
    void bob_shouldReadButNotWrite_resourceSharedDirectlyViaResourceGroupPermission() throws Exception {
        // Given: bob belongs to a group, and alice shares a resource with that group read-only
        UUID sharedGroupId = UUID.randomUUID();
        UUID readPermissionId = seedGroupMembership("bob", sharedGroupId, true, false);

        CreateResourceRequest createRequest = new CreateResourceRequest(
                "alice-shared-resource", "Shared directly via resource group permission.", null, "TEXT", null, null,
                List.of(new ResourceGroupPermissionRequest(sharedGroupId, readPermissionId)));
        String response = mockMvc.perform(asUser(post(RESOURCES_PATH), "alice", "resources.write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String resourceId = objectMapper.readTree(response).get("id").asText();

        // When & Then: bob can read it via the resource-level group grant...
        mockMvc.perform(asUser(get(RESOURCES_PATH + "/" + resourceId), "bob", "resources.read"))
                .andExpect(status().isOk());

        // ...but the grant is read-only, so he cannot rename it
        mockMvc.perform(asUser(patch(RESOURCES_PATH + "/" + resourceId), "bob", "resources.write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"hijacked\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void bob_shouldWrite_resourceSharedDirectlyViaResourceGroupPermission_withWriteGrant() throws Exception {
        // Given: alice shares a resource with bob's group, granting write this time
        UUID sharedGroupId = UUID.randomUUID();
        UUID writePermissionId = seedGroupMembership("bob", sharedGroupId, true, true);

        CreateResourceRequest createRequest = new CreateResourceRequest(
                "alice-writable-shared-resource", "Shared with write rights.", null, "TEXT", null, null,
                List.of(new ResourceGroupPermissionRequest(sharedGroupId, writePermissionId)));
        String response = mockMvc.perform(asUser(post(RESOURCES_PATH), "alice", "resources.write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String resourceId = objectMapper.readTree(response).get("id").asText();

        // When & Then: bob, granted write through the resource-level group permission, can rename it
        mockMvc.perform(asUser(patch(RESOURCES_PATH + "/" + resourceId), "bob", "resources.write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"renamed-by-bob\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void bob_shouldReadResource_inFolderSharedWithHisGroup_withNoDirectResourceGrant() throws Exception {
        // Given: alice shares a folder (not the resource itself) with bob's group
        seedGroupMembership("bob", READ_GROUP_ID, true, false);
        String folderResponse = mockMvc.perform(asUser(post(FOLDERS_PATH), "alice", "folders.write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FolderRequest("shared-folder-for-resource", "ROOT", List.of(READ_GROUP_ID)))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String folderId = objectMapper.readTree(folderResponse).get("id").asText();

        CreateResourceRequest resourceRequest = new CreateResourceRequest(
                "resource-in-shared-folder", "Visible to bob only via folder sharing.", null, "TEXT", null,
                UUID.fromString(folderId), null);
        String resourceResponse = mockMvc.perform(asUser(post(RESOURCES_PATH), "alice", "resources.write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resourceRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String resourceId = objectMapper.readTree(resourceResponse).get("id").asText();

        // When & Then: bob can read the resource purely through his group's folder access,
        // with no resource_group_permission row involved at all
        mockMvc.perform(asUser(get(RESOURCES_PATH + "/" + resourceId), "bob", "resources.read"))
                .andExpect(status().isOk());
    }

    // ---- searchResources (GET /resources/search) access-control filtering (searchWithAccessControl SQL path) ----

    @Test
    void searchResourcesByName_shouldExcludeOtherUsersPrivateResource() throws Exception {
        // Given: alice creates a private resource
        CreateResourceRequest request = new CreateResourceRequest(
                "alice-searchable-by-name", "Content only alice owns.", null, "TEXT", null, null, null);
        mockMvc.perform(asUser(post(RESOURCES_PATH), "alice", "resources.write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // When & Then: bob searching by that exact name finds nothing
        mockMvc.perform(asUser(get(RESOURCES_PATH + "/search").param("name", "alice-searchable-by-name"), "bob", "resources.read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void searchResourcesByName_shouldIncludeResource_whenSharedViaResourceGroupPermission() throws Exception {
        // Given: alice shares a resource with bob's group
        UUID sharedGroupId = UUID.randomUUID();
        UUID readPermissionId = seedGroupMembership("bob", sharedGroupId, true, false);

        CreateResourceRequest request = new CreateResourceRequest(
                "alice-shared-by-name", "Content shared with bob's group.", null, "TEXT", null, null,
                List.of(new ResourceGroupPermissionRequest(sharedGroupId, readPermissionId)));
        mockMvc.perform(asUser(post(RESOURCES_PATH), "alice", "resources.write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // When & Then: bob's search by name now finds it
        mockMvc.perform(asUser(get(RESOURCES_PATH + "/search").param("name", "alice-shared-by-name"), "bob", "resources.read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("alice-shared-by-name")));
    }

    // ---- Semantic search access-control filtering (own SQL path, distinct from findByIdWithAccessControl) ----

    @Test
    void semanticSearch_shouldExcludeOtherUsersPrivateResource_andIncludeGroupSharedOne() throws Exception {
        // Given: alice's private resource — bob must never see it in search results
        CreateResourceRequest privateRequest = new CreateResourceRequest(
                "alice-private-searchable", "Alice private content about telescopes.", null, "TEXT", null, null, null);
        String privateResponse = mockMvc.perform(asUser(post(RESOURCES_PATH), "alice", "resources.write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(privateRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String privateResourceId = objectMapper.readTree(privateResponse).get("id").asText();

        // And a resource alice shares directly with bob's group
        UUID sharedGroupId = UUID.randomUUID();
        UUID readPermissionId = seedGroupMembership("bob", sharedGroupId, true, false);
        CreateResourceRequest sharedRequest = new CreateResourceRequest(
                "alice-shared-searchable", "Alice shared content about telescopes.", null, "TEXT", null, null,
                List.of(new ResourceGroupPermissionRequest(sharedGroupId, readPermissionId)));
        String sharedResponse = mockMvc.perform(asUser(post(RESOURCES_PATH), "alice", "resources.write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sharedRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String sharedResourceId = objectMapper.readTree(sharedResponse).get("id").asText();

        SearchRequest searchRequest = new SearchRequest("telescopes", 10, null, null);

        // When & Then: bob's search surfaces the shared resource, never alice's private one
        mockMvc.perform(asUser(post("/api/v1/search/semantic"), "bob", "search.semantic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.resource_id=='" + sharedResourceId + "')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.resource_id=='" + privateResourceId + "')]", hasSize(0)));
    }

    private void seedReadOnlyGroupMembership(String username, UUID groupId) {
        seedGroupMembership(username, groupId, true, false);
    }

    private UUID seedGroupMembership(String username, UUID groupId, boolean canRead, boolean canWrite) {
        UUID permissionId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO referential.groups (id, name, description) VALUES (?, ?, 'test group')
                ON CONFLICT DO NOTHING
                """, groupId, "group-" + groupId);
        jdbcTemplate.update("""
                INSERT INTO referential.permissions (id, name, description, can_read, can_write, is_admin)
                VALUES (?, ?, 'test permission', ?, ?, false)
                """, permissionId, "perm-" + permissionId, canRead, canWrite);
        jdbcTemplate.update("""
                INSERT INTO referential.group_users (group_id, user_id, permission_id)
                SELECT ?, u.id, ? FROM referential.users u WHERE u.username = ?
                ON CONFLICT DO NOTHING
                """, groupId, permissionId, username);
        return permissionId;
    }
}
