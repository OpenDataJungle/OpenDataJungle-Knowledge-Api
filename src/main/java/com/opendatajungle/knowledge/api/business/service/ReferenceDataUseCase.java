package com.opendatajungle.knowledge.api.business.service;

import java.util.List;
import java.util.UUID;

public interface ReferenceDataUseCase {
    boolean hasCurrentUserWriteGroupAccess(List<UUID> groupIds);
}
