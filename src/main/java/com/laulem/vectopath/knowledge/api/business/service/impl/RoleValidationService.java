package com.laulem.vectopath.knowledge.api.business.service.impl;

import com.laulem.vectopath.knowledge.api.business.exception.ParamException;
import com.laulem.vectopath.knowledge.api.business.model.SecurityConfig;
import com.laulem.vectopath.knowledge.api.business.service.AuthenticationUseCase;
import com.laulem.vectopath.knowledge.api.business.service.RoleValidationUseCase;
import com.laulem.vectopath.knowledge.api.shared.util.CollectionUtils;

import java.util.List;

public class RoleValidationService implements RoleValidationUseCase {
    private final AuthenticationUseCase authenticationUseCase;
    private final SecurityConfig securityConfig;

    public RoleValidationService(AuthenticationUseCase authenticationUseCase,
                                 SecurityConfig securityConfig) {
        this.authenticationUseCase = authenticationUseCase;
        this.securityConfig = securityConfig;
    }

    @Override
    public void validateAllowedRoles(List<String> allowedRoles) {
        if (CollectionUtils.isEmpty(allowedRoles)) {
            return;
        }

        List<String> userAuthorities = authenticationUseCase.getAuthorities();
        if (userAuthorities.contains(securityConfig.getAdminRole())) {
            return;
        }

        List<String> notAffectableRoles = securityConfig.getNotAffectableRoles();
        boolean containsForbiddenRoles = allowedRoles.stream().anyMatch(notAffectableRoles::contains);
        if (containsForbiddenRoles) {
            throw new ParamException("FORBIDDEN_ROLES", "You cannot assign protected roles: " + String.join(", ", notAffectableRoles), "allowedRoles");
        }

        boolean containsUnauthorizedRoles = allowedRoles.stream().anyMatch(role -> !userAuthorities.contains(role));
        if (containsUnauthorizedRoles) {
            throw new ParamException("UNAUTHORIZED_ROLES", "You can only assign roles that you possess.", "allowedRoles");
        }
    }
}

