package com.fluxpay.subscription.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fluxpay.common.entity.BaseEntity;
import com.fluxpay.common.enums.SubscriptionStatus;
import com.fluxpay.security.context.TenantContext;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
public class Subscription extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status = SubscriptionStatus.INCOMPLETE;

    @Column(name = "current_period_start", nullable = false)
    private Instant currentPeriodStart;

    @Column(name = "current_period_end", nullable = false)
    private Instant currentPeriodEnd;

    @Column(name = "trial_start")
    private Instant trialStart;

    @Column(name = "trial_end")
    private Instant trialEnd;

    @Column(name = "cancel_at")
    private Instant cancelAt;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "billing_cycle_anchor", nullable = false)
    private Instant billingCycleAnchor;

    @Column(name = "collection_method", nullable = false, length = 20)
    private String collectionMethod = "charge_automatically";

    @Column(name = "proration_behavior", nullable = false, length = 20)
    private String prorationBehavior = "create_prorations";

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @PrePersist
    public void prePersist() {
        if (this.tenantId == null) {
            this.tenantId = TenantContext.getCurrentTenantId();
        }
    }

    public boolean isInTrial() {
        if (trialEnd == null) {
            return false;
        }
        return Instant.now().isBefore(trialEnd);
    }
}

