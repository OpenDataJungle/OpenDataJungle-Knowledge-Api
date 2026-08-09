package com.opendatajungle.knowledge.api.infra.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceGroupPermissionIdEmbeddable implements Serializable {

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "group_id")
    private UUID groupId;
}
