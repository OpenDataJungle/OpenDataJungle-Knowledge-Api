package com.laulem.vectopath.knowledge.api.infra.entity;

import com.laulem.vectopath.knowledge.api.shared.util.DateUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "app_roles", schema = "knowledge")
public class RoleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "role_name", nullable = false, unique = true, length = 100)
    private String roleName;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public RoleEntity() {
        this.createdAt = DateUtils.now();
    }

    public RoleEntity(String roleName, String description) {
        this.roleName = roleName;
        this.description = description;
        this.createdAt = DateUtils.now();
    }
}
