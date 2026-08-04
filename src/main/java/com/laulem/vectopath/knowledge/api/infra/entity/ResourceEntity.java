package com.laulem.vectopath.knowledge.api.infra.entity;

import com.laulem.vectopath.knowledge.api.business.model.Resource;
import com.laulem.vectopath.knowledge.api.business.model.ResourceGroupPermission;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "resource", schema = "knowledge")
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
    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "source_name")
    private String sourceName;

    @Column(name = "size")
    private Long size;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "folder_id", columnDefinition = "UUID")
    private UUID folderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", insertable = false, updatable = false)
    private FolderEntity folder;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "resource")
    private Set<ResourceGroupPermissionEntity> groupPermissions = new HashSet<>();

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
        entity.folderId = resource.getFolderId();
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

    @PreUpdate
    protected void onUpdate() {
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
        resource.setFolderId(this.folderId);
        resource.setGroupPermissions(Optional.ofNullable(this.groupPermissions).orElse(Collections.emptySet()).stream()
                .map(gp -> new ResourceGroupPermission(gp.getId().getGroupId(), gp.getPermissionId()))
                .toList());
        resource.setCreatedAt(this.createdAt);
        resource.setUpdatedAt(this.updatedAt);
        return resource;
    }
}
