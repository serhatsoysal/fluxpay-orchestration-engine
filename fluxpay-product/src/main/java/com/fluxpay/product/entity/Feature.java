package com.fluxpay.product.entity;

import com.fluxpay.common.entity.BaseEntity;
import com.fluxpay.security.context.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "features", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "feature_key"})
})
@Getter
@Setter
public class Feature extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "feature_key", nullable = false, length = 100)
    private String featureKey;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(name = "default_value")
    private Integer defaultValue;

    @PrePersist
    public void prePersist() {
        if (this.tenantId == null) {
            this.tenantId = TenantContext.getCurrentTenantId();
        }
    }
}

