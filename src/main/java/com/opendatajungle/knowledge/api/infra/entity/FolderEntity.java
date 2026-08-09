package com.opendatajungle.knowledge.api.infra.entity;

import com.opendatajungle.knowledge.api.business.model.Folder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "folder", schema = "knowledge")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false, unique = true)
    private String completePath;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "parent_id", columnDefinition = "UUID")
    private UUID parentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    private FolderEntity parent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", insertable = false, updatable = false)
    private Set<ResourceEntity> resources;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "folder_group",
        schema = "knowledge",
        joinColumns = @JoinColumn(name = "folder_id"),
        inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<GroupEntity> groups;

    public static FolderEntity fromDomain(Folder folder) {
        return FolderEntity.builder()
                .id(folder.getId())
                .name(folder.getName())
                .path(folder.getPath())
                .completePath(folder.getCompletePath())
                .parentId(folder.getParentId())
                .createdBy(folder.getCreatedBy())
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .build();
    }

    public Folder toDomain() {
        Folder folder = new Folder();
        folder.setId(this.id);
        folder.setName(this.name);
        folder.setPath(this.path);
        folder.setParentId(this.parentId);
        folder.setCreatedBy(this.createdBy);
        folder.setCreatedAt(this.createdAt);
        folder.setUpdatedAt(this.updatedAt);
        folder.setGroupIds(this.groups != null ? this.groups.stream().map(GroupEntity::getId).toList() : null);
        return folder;
    }
}
