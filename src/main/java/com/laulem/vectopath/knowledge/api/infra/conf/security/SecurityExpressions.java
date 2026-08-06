package com.laulem.vectopath.knowledge.api.infra.conf.security;

public final class SecurityExpressions {

    // Search scopes
    public static final String SEARCH_SEMANTIC = "hasAuthority(@securityScopesProperties.search.semantic)";
    // Resources scopes
    public static final String RESOURCES_READ = "hasAuthority(@securityScopesProperties.resources.read)";
    public static final String RESOURCES_WRITE = "hasAuthority(@securityScopesProperties.resources.write)";
    public static final String RESOURCES_DELETE = "hasAuthority(@securityScopesProperties.resources.delete)";
    // Folders scopes
    public static final String FOLDERS_READ = "hasAuthority(@securityScopesProperties.folders.read)";
    public static final String FOLDERS_WRITE = "hasAuthority(@securityScopesProperties.folders.write)";
    public static final String FOLDERS_DELETE = "hasAuthority(@securityScopesProperties.folders.delete)";

    private SecurityExpressions() {
    }
}
