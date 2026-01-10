package com.fluxpay.tenant.entity;

import com.fluxpay.common.entity.BaseEntity;
import com.fluxpay.security.context.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
public class Notification extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private Boolean read = false;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "type", length = 50)
    private String type;

    @PrePersist
    public void prePersist() {
        if (this.tenantId == null) {
            this.tenantId = TenantContext.getCurrentTenantId();
        }
    }
}

