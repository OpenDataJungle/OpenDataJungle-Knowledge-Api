package com.laulem.vectopath.knowledge.api.infra.entity;

import com.laulem.vectopath.knowledge.api.business.model.Resource;
import com.laulem.vectopath.knowledge.api.business.model.ResourceStatus;
import com.laulem.vectopath.knowledge.api.shared.util.DateUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "resources", schema = "knowledge")
public class ResourceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_type")
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private String metadata;

    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "source_name")
    private String sourceName;

    @Column(name = "size")
    private Long size;

    @Column(name = "created_by")
    private String createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false)
    private Resource.AccessLevel accessLevel;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "resource_allowed_roles",
            schema = "knowledge",
            joinColumns = @JoinColumn(name = "resource_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private List<RoleEntity> allowedRoles = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static ResourceEntity fromDomain(Resource resource) {
        ResourceEntity entity = new ResourceEntity();
        if (resource.getId() != null) {
            entity.id = resource.getId();
        }
        entity.name = resource.getName();
        entity.content = resource.getContent();
        entity.contentType = resource.getContentType();
        entity.status = resource.getStatus();
        entity.metadata = resource.getMetadata();
        entity.sourceType = resource.getSourceType();
        entity.sourceName = resource.getSourceName();
        entity.size = resource.getSize();
        entity.createdBy = resource.getCreatedBy();
        entity.accessLevel = resource.getAccessLevel();
        entity.createdAt = resource.getCreatedAt();
        entity.updatedAt = resource.getUpdatedAt();
        return entity;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = DateUtils.now();
        }
        this.updatedAt = DateUtils.now();
    }

    public Resource toDomain() {
        Resource resource = new Resource();
        resource.setId(this.id);
        resource.setName(this.name);
        resource.setContent(this.content);
        resource.setContentType(this.contentType);
        resource.setStatus(this.status);
        resource.setMetadata(this.metadata);
        resource.setSourceType(this.sourceType);
        resource.setSourceName(this.sourceName);
        resource.setSize(this.size);
        resource.setCreatedBy(this.createdBy);
        resource.setAccessLevel(this.accessLevel);
        resource.setAllowedRoles(
                this.allowedRoles.stream()
                        .map(RoleEntity::getRoleName)
                        .toList()
        );
        resource.setCreatedAt(this.createdAt);
        resource.setUpdatedAt(this.updatedAt);
        return resource;
    }
}
