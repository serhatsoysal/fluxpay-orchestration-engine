package com.fluxpay.billing.entity;

import com.fluxpay.common.entity.BaseEntity;
import com.fluxpay.security.context.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "dunning_configs")
@Getter
@Setter
public class DunningConfig extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "max_retry_attempts", nullable = false)
    private Integer maxRetryAttempts = 3;

    @Column(name = "retry_interval_days", nullable = false)
    private Integer retryIntervalDays = 3;

    @Column(name = "send_email_notifications", nullable = false)
    private Boolean sendEmailNotifications = true;

    @Column(nullable = false)
    private Boolean active = true;

    @PrePersist
    public void prePersist() {
        if (this.tenantId == null) {
            this.tenantId = TenantContext.getCurrentTenantId();
        }
    }
}

