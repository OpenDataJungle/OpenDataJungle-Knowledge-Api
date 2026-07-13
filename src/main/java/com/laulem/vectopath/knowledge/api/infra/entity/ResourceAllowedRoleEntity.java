package com.laulem.vectopath.knowledge.api.infra.entity;

import com.laulem.vectopath.knowledge.api.shared.util.DateUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "resource_allowed_roles", schema = "knowledge")
@IdClass(ResourceAllowedRoleEntity.ResourceAllowedRoleId.class)
public class ResourceAllowedRoleEntity {
    @Id
    @Column(name = "resource_id")
    private UUID resourceId;

    @Id
    @Column(name = "role_id")
    private Integer roleId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ResourceAllowedRoleEntity() {
        this.createdAt = DateUtils.now();
    }

    public ResourceAllowedRoleEntity(UUID resourceId, Integer roleId) {
        this.resourceId = resourceId;
        this.roleId = roleId;
        this.createdAt = DateUtils.now();
    }

    // Getters and setters
    public UUID getResourceId() {
        return resourceId;
    }

    public void setResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Composite ID class
    public static class ResourceAllowedRoleId implements Serializable {
        private UUID resourceId;
        private Integer roleId;

        public ResourceAllowedRoleId() {
        }

        public ResourceAllowedRoleId(UUID resourceId, Integer roleId) {
            this.resourceId = resourceId;
            this.roleId = roleId;
        }

        public UUID getResourceId() {
            return resourceId;
        }

        public void setResourceId(UUID resourceId) {
            this.resourceId = resourceId;
        }

        public Integer getRoleId() {
            return roleId;
        }

        public void setRoleId(Integer roleId) {
            this.roleId = roleId;
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof final ResourceAllowedRoleId that)) return false;
            return Objects.equals(resourceId, that.resourceId) && Objects.equals(roleId, that.roleId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(resourceId, roleId);
        }
    }
}
