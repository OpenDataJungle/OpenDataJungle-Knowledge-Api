package com.opendatajungle.knowledge.api.infra.service;

import com.opendatajungle.knowledge.api.business.service.AuthenticationUseCase;
import com.opendatajungle.knowledge.api.infra.repository.ReferentialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferentialServiceTest {

    @Mock
    private ReferentialRepository referentialRepository;

    @Mock
    private AuthenticationUseCase authenticationUseCase;

    @InjectMocks
    private ReferentialService service;

    @Test
    void hasCurrentUserWriteGroupAccess_shouldReturnTrue_whenRepositoryGrantsAccess() {
        // Given
        List<UUID> groupIds = List.of(UUID.randomUUID());
        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");
        when(referentialRepository.hasGroupWriteAccess(groupIds, "alice")).thenReturn(true);

        // When
        boolean result = service.hasCurrentUserWriteGroupAccess(groupIds);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void hasCurrentUserWriteGroupAccess_shouldReturnFalse_whenRepositoryDeniesAccess() {
        // Given
        List<UUID> groupIds = List.of(UUID.randomUUID());
        when(authenticationUseCase.getCurrentUser()).thenReturn("bob");
        when(referentialRepository.hasGroupWriteAccess(groupIds, "bob")).thenReturn(false);

        // When
        boolean result = service.hasCurrentUserWriteGroupAccess(groupIds);

        // Then
        assertThat(result).isFalse();
    }
}
