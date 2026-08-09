package com.opendatajungle.knowledge.api.infra.service;

import com.opendatajungle.knowledge.api.business.service.AuthenticationUseCase;
import com.opendatajungle.knowledge.api.business.service.ReferentialUseCase;
import com.opendatajungle.knowledge.api.infra.repository.ReferentialRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ReferentialService implements ReferentialUseCase {
    private final ReferentialRepository referentialRepository;
    private final AuthenticationUseCase authenticationUseCase;

    public ReferentialService(ReferentialRepository referentialRepository, AuthenticationUseCase authenticationUseCase) {
        this.referentialRepository = referentialRepository;
        this.authenticationUseCase = authenticationUseCase;
    }

    @Override
    public boolean hasCurrentUserWriteGroupAccess(final List<UUID> groupIds) {
        return referentialRepository.hasGroupWriteAccess(groupIds, authenticationUseCase.getCurrentUser());
    }
}
