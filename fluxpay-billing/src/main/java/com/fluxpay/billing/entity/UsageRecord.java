package com.fluxpay.billing.entity;

import com.fluxpay.common.entity.BaseEntity;
import com.fluxpay.security.context.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usage_records")
@Getter
@Setter
public class UsageRecord extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "subscription_item_id", nullable = false)
    private UUID subscriptionItemId;

    @Column(name = "meter_name", nullable = false, length = 100)
    private String meterName;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(length = 255)
    private String action;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @PrePersist
    public void prePersist() {
        if (this.tenantId == null) {
            this.tenantId = TenantContext.getCurrentTenantId();
        }
        if (this.timestamp == null) {
            this.timestamp = Instant.now();
        }
    }
}

