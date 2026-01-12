package com.fluxpay.tenant.entity;

import com.fluxpay.common.entity.BaseEntity;
import com.fluxpay.common.enums.Currency;
import com.fluxpay.common.enums.TenantStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "tenants")
@Getter
@Setter
public class Tenant extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status = TenantStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_currency")
    private Currency defaultCurrency = Currency.USD;

    @Column(name = "subscription_tier", nullable = false, length = 50)
    private String subscriptionTier = "starter";

    @Column(name = "subscription_expires_at")
    private Instant subscriptionExpiresAt;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "primary_color", length = 7)
    private String primaryColor;

    @Column(name = "support_email")
    private String supportEmail;

    @Column(name = "billing_email", nullable = false)
    private String billingEmail;

    @Column(name = "max_users", nullable = false)
    private Integer maxUsers = 5;

    @Column(name = "max_api_calls_per_month", nullable = false)
    private Integer maxApiCallsPerMonth = 10000;

    public boolean isActive() {
        return status == TenantStatus.ACTIVE;
    }
}

