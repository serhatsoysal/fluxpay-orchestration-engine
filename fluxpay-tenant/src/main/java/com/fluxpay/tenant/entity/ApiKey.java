package com.fluxpay.tenant.entity;

import com.fluxpay.common.entity.BaseEntity;
import com.fluxpay.common.enums.ApiKeyScope;
import com.fluxpay.security.context.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_keys")
@Getter
@Setter
public class ApiKey extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "key_prefix", nullable = false, length = 10)
    private String keyPrefix;

    @Column(name = "key_hash", nullable = false, unique = true)
    private String keyHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApiKeyScope scope = ApiKeyScope.READ_ONLY;

    @Column(name = "rate_limit_per_minute", nullable = false)
    private Integer rateLimitPerMinute = 100;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @PrePersist
    public void prePersist() {
        if (this.tenantId == null) {
            this.tenantId = TenantContext.getCurrentTenantId();
        }
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }
}

