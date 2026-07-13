package com.laulem.vectopath.knowledge.api.business.service;

import java.util.List;
import java.util.Optional;

public interface AuthenticationUseCase {
    String DEFAULT_UNKNOWN_USERNAME = "anonymous";

    String getCurrentUser();

    Optional<String> findCurrentUser();

    List<String> getAuthorities();
}
