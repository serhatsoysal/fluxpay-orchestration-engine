package com.fluxpay.product.entity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fluxpay.common.entity.BaseEntity;
import com.fluxpay.common.enums.BillingInterval;
import com.fluxpay.common.enums.PricingModel;
import com.fluxpay.common.enums.UsageAggregationType;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "prices")
@Getter
@Setter
public class Price extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_model", nullable = false)
    private PricingModel pricingModel = PricingModel.FLAT_RATE;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval", nullable = false)
    private BillingInterval billingInterval = BillingInterval.MONTHLY;

    @Column(name = "unit_amount", nullable = false, precision = 12, scale = 4)
    private BigDecimal unitAmount;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, Object>> tiers;

    @Column(name = "trial_period_days")
    private Integer trialPeriodDays;

    @Column(name = "meter_name", length = 100)
    private String meterName;

    @Enumerated(EnumType.STRING)
    @Column(name = "aggregation_type")
    private UsageAggregationType aggregationType;

    @Column(nullable = false)
    private Boolean active = true;
}

