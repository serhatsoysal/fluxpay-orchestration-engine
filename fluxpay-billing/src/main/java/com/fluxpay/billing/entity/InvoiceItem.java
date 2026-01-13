package com.fluxpay.billing.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fluxpay.common.entity.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "invoice_items")
@Getter
@Setter
public class InvoiceItem extends BaseEntity {

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", insertable = false, updatable = false)
    private Invoice invoice;

    @Column(name = "subscription_item_id")
    private UUID subscriptionItemId;

    @Column(name = "price_id")
    private UUID priceId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_amount", nullable = false)
    private Long unitAmount;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "is_proration", nullable = false)
    private Boolean isProration = false;

    @Type(JsonBinaryType.class)
    @Column(name = "proration_details", columnDefinition = "jsonb")
    private Map<String, Object> prorationDetails;

    @Column(name = "period_start")
    private Instant periodStart;

    @Column(name = "period_end")
    private Instant periodEnd;
}

