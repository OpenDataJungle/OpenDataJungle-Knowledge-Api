package com.opendatajungle.knowledge.api.infra.repository;

import com.opendatajungle.knowledge.api.business.service.AuthenticationUseCase;
import com.opendatajungle.knowledge.api.infra.dto.GroupResponse;
import com.opendatajungle.knowledge.api.infra.dto.GroupUserResponse;
import com.opendatajungle.knowledge.api.infra.dto.PaginatedResponse;
import com.opendatajungle.knowledge.api.infra.dto.PermissionResponse;
import com.opendatajungle.knowledge.api.infra.dto.UserResponse;
import com.opendatajungle.knowledge.api.infra.properties.OpenDataJungleReferentialApiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
class ReferentialRepositoryTest {

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec uriSpec;

    @Mock
    private RestClient.RequestHeadersSpec headersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Mock
    private AuthenticationUseCase authenticationUseCase;

    private ReferentialRepository repository;

    @BeforeEach
    void setUp() {
        OpenDataJungleReferentialApiProperties properties = new OpenDataJungleReferentialApiProperties();
        properties.setBaseUrl("https://laulem.com");
        properties.setUserByUsername("/users/by-username/{username}");
        properties.setUserGroupsByUserId("/users/{userId}/groups");

        when(restClientBuilder.baseUrl("https://laulem.com")).thenReturn(restClientBuilder);
        when(restClientBuilder.build()).thenReturn(restClient);

        // ReferentialRepository's constructor calls restClientBuilder.baseUrl(...).build() directly,
        // so it must be built after the builder stubs above are in place (@InjectMocks would run too early).
        repository = new ReferentialRepository(restClientBuilder, properties, authenticationUseCase);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenReturn(headersSpec);
        when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(authenticationUseCase.getToken()).thenReturn(Optional.of("bearer-token"));
    }

    @Test
    void hasGroupWriteAccess_shouldReturnFalse_whenUserLookupReturnsNoUser() {
        // Given
        when(responseSpec.body(UserResponse.class)).thenReturn(null);

        // When
        boolean result = repository.hasGroupWriteAccess(List.of(UUID.randomUUID()), "alice");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void hasGroupWriteAccess_shouldReturnTrue_whenGroupGrantsWritePermission() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        stubUserLookup(userId);
        stubGroupsLookup(List.of(groupUser(groupId, true, false)));

        // When
        boolean result = repository.hasGroupWriteAccess(List.of(groupId), "alice");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void hasGroupWriteAccess_shouldReturnTrue_whenGroupGrantsAdminPermission() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        stubUserLookup(userId);
        stubGroupsLookup(List.of(groupUser(groupId, false, true)));

        // When
        boolean result = repository.hasGroupWriteAccess(List.of(groupId), "alice");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void hasGroupWriteAccess_shouldReturnFalse_whenNoRequestedGroupMatches() {
        // Given
        UUID userId = UUID.randomUUID();
        stubUserLookup(userId);
        stubGroupsLookup(List.of(groupUser(UUID.randomUUID(), true, false)));

        // When
        boolean result = repository.hasGroupWriteAccess(List.of(UUID.randomUUID()), "alice");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void hasGroupWriteAccess_shouldReturnFalse_whenRestCallThrows() {
        // Given
        UUID userId = UUID.randomUUID();
        stubUserLookup(userId);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenThrow(new RuntimeException("service down"));

        // When
        boolean result = repository.hasGroupWriteAccess(List.of(UUID.randomUUID()), "alice");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void getGroupWriteAccess_shouldReturnEmptyList_whenUserLookupReturnsNoUser() {
        // Given
        when(responseSpec.body(UserResponse.class)).thenReturn(null);

        // When
        List<UUID> result = repository.getGroupWriteAccess("alice");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getGroupWriteAccess_shouldReturnOnlyWritableGroupIds() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID writableGroupId = UUID.randomUUID();
        UUID readOnlyGroupId = UUID.randomUUID();
        stubUserLookup(userId);
        stubGroupsLookup(List.of(
                groupUser(writableGroupId, true, false),
                groupUser(readOnlyGroupId, false, false)));

        // When
        List<UUID> result = repository.getGroupWriteAccess("alice");

        // Then
        assertThat(result).containsExactly(writableGroupId);
    }

    @Test
    void getGroupWriteAccess_shouldReturnEmptyList_whenRestCallThrows() {
        // Given
        UUID userId = UUID.randomUUID();
        stubUserLookup(userId);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenThrow(new RuntimeException("service down"));

        // When
        List<UUID> result = repository.getGroupWriteAccess("alice");

        // Then
        assertThat(result).isEmpty();
    }

    private void stubUserLookup(UUID userId) {
        UserResponse userResponse = new UserResponse();
        userResponse.setId(userId);
        when(responseSpec.body(UserResponse.class)).thenReturn(userResponse);
    }

    private void stubGroupsLookup(List<GroupUserResponse> content) {
        PaginatedResponse<GroupUserResponse> page = new PaginatedResponse<>(content, content.size(), 1, 0, content.size());
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(page);
    }

    private static GroupUserResponse groupUser(UUID groupId, boolean canWrite, boolean isAdmin) {
        GroupResponse group = new GroupResponse();
        group.setId(groupId);
        PermissionResponse permission = new PermissionResponse();
        permission.setCanWrite(canWrite);
        permission.setIsAdmin(isAdmin);
        GroupUserResponse groupUser = new GroupUserResponse();
        groupUser.setGroup(group);
        groupUser.setPermission(permission);
        return groupUser;
    }
}
