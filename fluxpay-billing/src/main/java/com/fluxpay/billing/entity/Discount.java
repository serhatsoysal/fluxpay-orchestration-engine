package com.fluxpay.billing.entity;

import com.fluxpay.common.entity.BaseEntity;
import com.fluxpay.security.context.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "discounts")
@Getter
@Setter
public class Discount extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "coupon_id", nullable = false)
    private UUID couponId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "subscription_id")
    private UUID subscriptionId;

    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Column(name = "discount_amount", nullable = false)
    private Long discountAmount;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @PrePersist
    public void prePersist() {
        if (this.tenantId == null) {
            this.tenantId = TenantContext.getCurrentTenantId();
        }
        if (this.appliedAt == null) {
            this.appliedAt = Instant.now();
        }
    }
}

