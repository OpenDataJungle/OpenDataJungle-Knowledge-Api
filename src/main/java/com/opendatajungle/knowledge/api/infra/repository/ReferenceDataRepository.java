package com.opendatajungle.knowledge.api.infra.repository;

import com.opendatajungle.commons.business.service.AuthenticationUseCase;
import com.opendatajungle.knowledge.api.infra.dto.GroupUserResponse;
import com.opendatajungle.knowledge.api.infra.dto.PaginatedResponse;
import com.opendatajungle.knowledge.api.infra.dto.UserResponse;
import com.opendatajungle.knowledge.api.infra.properties.OpenDataJungleReferenceDataApiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Slf4j
public class ReferenceDataRepository {
    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer ";
    private final RestClient restClient;
    private final OpenDataJungleReferenceDataApiProperties openDataJungleReferenceDataApiProperties;
    private final AuthenticationUseCase authenticationUseCase;

    public ReferenceDataRepository(RestClient.Builder restClientBuilder, OpenDataJungleReferenceDataApiProperties openDataJungleReferenceDataApiProperties, AuthenticationUseCase authenticationUseCase) {
        this.restClient = restClientBuilder.baseUrl(openDataJungleReferenceDataApiProperties.getBaseUrl()).build();
        this.openDataJungleReferenceDataApiProperties = openDataJungleReferenceDataApiProperties;
        this.authenticationUseCase = authenticationUseCase;
    }

    public boolean hasGroupWriteAccess(final List<UUID> groupIds, final String currentUser) {
        UUID userId = this.getUserId(currentUser);
        if (userId == null) {
            return false;
        }

        try {
            String bearerToken = authenticationUseCase.getToken().orElse("");

            PaginatedResponse<GroupUserResponse> response = restClient.get()
                    .uri(this.openDataJungleReferenceDataApiProperties.getUserGroupsByUserId(), userId)
                    .header(AUTHORIZATION, BEARER + bearerToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<PaginatedResponse<GroupUserResponse>>() {
                    });

            return Optional.ofNullable(response).map(PaginatedResponse::content).orElse(List.of()).stream()
                    .anyMatch(groupUser -> groupIds.contains(groupUser.getGroup().getId())
                            && (groupUser.getPermission().getCanWrite() || groupUser.getPermission().getIsAdmin()));
        } catch (Exception e) {
            log.error("Error while checking group write access for user {}: {}", currentUser, e.getMessage(), e);
            return false;
        }
    }

    public List<UUID> getGroupWriteAccess(final String currentUser) {
        UUID userId = this.getUserId(currentUser);
        if (userId == null) {
            return List.of();
        }

        try {
            String bearerToken = authenticationUseCase.getToken().orElse("");

            PaginatedResponse<GroupUserResponse> response = restClient.get()
                    .uri(this.openDataJungleReferenceDataApiProperties.getUserGroupsByUserId(), userId)
                    .header(AUTHORIZATION, BEARER + bearerToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<PaginatedResponse<GroupUserResponse>>() {
                    });

            return Optional.ofNullable(response).map(PaginatedResponse::content).orElse(List.of()).stream()
                    .filter(groupUser -> groupUser.getPermission().getCanWrite() || groupUser.getPermission().getIsAdmin())
                    .map(groupUser -> groupUser.getGroup().getId())
                    .toList();
        } catch (Exception e) {
            log.error("Error while getting group write access for user {}: {}", currentUser, e.getMessage(), e);
            return List.of();
        }
    }

    private UUID getUserId(final String currentUser) {
        try {
            String bearerToken = authenticationUseCase.getToken().orElse("");

            UserResponse userResponse = restClient.get()
                    .uri(this.openDataJungleReferenceDataApiProperties.getUserByUsername(), currentUser)
                    .header(AUTHORIZATION, BEARER + bearerToken)
                    .retrieve()
                    .body(UserResponse.class);

            return userResponse != null ? userResponse.getId() : null;
        } catch (Exception e) {
            log.error("Error while getting user ID for user {}: {}", currentUser, e.getMessage(), e);
            return null;
        }
    }
}
