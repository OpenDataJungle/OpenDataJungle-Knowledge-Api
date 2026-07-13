package com.laulem.vectopath.knowledge.api.infra.properties;

import com.laulem.vectopath.knowledge.api.business.model.SecurityConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties implements SecurityConfig {
    private String adminRole;
    private List<String> notAffectableRoles;

    public String getAdminRole() {
        return adminRole;
    }

    public void setAdminRole(String adminRole) {
        this.adminRole = adminRole;
    }

    public List<String> getNotAffectableRoles() {
        return notAffectableRoles;
    }

    public void setNotAffectableRoles(List<String> notAffectableRoles) {
        this.notAffectableRoles = notAffectableRoles;
    }
}
