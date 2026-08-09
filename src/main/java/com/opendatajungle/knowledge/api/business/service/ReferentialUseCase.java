package com.opendatajungle.knowledge.api.business.service;

import java.util.List;
import java.util.UUID;

public interface ReferentialUseCase {
    boolean hasCurrentUserWriteGroupAccess(List<UUID> groupIds);
}
