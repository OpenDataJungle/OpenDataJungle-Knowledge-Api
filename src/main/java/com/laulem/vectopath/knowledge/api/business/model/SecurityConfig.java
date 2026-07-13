package com.laulem.vectopath.knowledge.api.business.model;

import java.util.List;

public interface SecurityConfig {
    String getAdminRole();
    List<String> getNotAffectableRoles();
}
