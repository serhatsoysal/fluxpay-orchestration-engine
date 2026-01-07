package com.fluxpay.subscription.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fluxpay.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "subscription_items")
@Getter
@Setter
public class SubscriptionItem extends BaseEntity {

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", insertable = false, updatable = false)
    private Subscription subscription;

    @Column(name = "price_id", nullable = false)
    private UUID priceId;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "usage_quantity", precision = 12, scale = 4)
    private BigDecimal usageQuantity = BigDecimal.ZERO;
}

