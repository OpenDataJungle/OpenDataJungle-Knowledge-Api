package com.opendatajungle.knowledge.api.infra.service;

import com.opendatajungle.commons.business.service.AuthenticationUseCase;
import com.opendatajungle.knowledge.api.business.service.ReferenceDataUseCase;
import com.opendatajungle.knowledge.api.infra.repository.ReferenceDataRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ReferenceDataService implements ReferenceDataUseCase {
    private final ReferenceDataRepository referenceDataRepository;
    private final AuthenticationUseCase authenticationUseCase;

    public ReferenceDataService(ReferenceDataRepository referenceDataRepository, AuthenticationUseCase authenticationUseCase) {
        this.referenceDataRepository = referenceDataRepository;
        this.authenticationUseCase = authenticationUseCase;
    }

    @Override
    public boolean hasCurrentUserWriteGroupAccess(final List<UUID> groupIds) {
        return referenceDataRepository.hasGroupWriteAccess(groupIds, authenticationUseCase.getCurrentUser());
    }
}
