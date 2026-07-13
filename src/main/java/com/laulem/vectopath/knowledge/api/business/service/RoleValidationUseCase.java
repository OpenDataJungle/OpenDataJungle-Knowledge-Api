package com.laulem.vectopath.knowledge.api.business.service;

import java.util.List;

public interface RoleValidationUseCase {
    void validateAllowedRoles(List<String> allowedRoles);
}
