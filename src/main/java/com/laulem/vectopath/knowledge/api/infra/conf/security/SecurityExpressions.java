package com.laulem.vectopath.knowledge.api.infra.conf.security;

public final class SecurityExpressions {

    private SecurityExpressions() {
    }

    // Search scopes
    public static final String SEARCH_SEMANTIC = "hasAuthority(@securityScopesProperties.search.semantic)";

    // Resources scopes
    public static final String RESOURCES_READ = "hasAuthority(@securityScopesProperties.resources.read)";
    public static final String RESOURCES_WRITE = "hasAuthority(@securityScopesProperties.resources.write)";
    public static final String RESOURCES_DELETE = "hasAuthority(@securityScopesProperties.resources.delete)";
}
