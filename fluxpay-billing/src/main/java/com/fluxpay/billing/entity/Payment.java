package com.fluxpay.billing.entity;

import com.fluxpay.common.entity.BaseEntity;
import com.fluxpay.common.enums.PaymentStatus;
import com.fluxpay.security.context.TenantContext;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
public class Payment extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "processor_payment_id")
    private String processorPaymentId;

    @Column(name = "processor_name", nullable = false, length = 50)
    private String processorName;

    @Column(name = "payment_method_type", length = 50)
    private String paymentMethodType;

    @Type(JsonBinaryType.class)
    @Column(name = "payment_method_details", columnDefinition = "jsonb")
    private Map<String, Object> paymentMethodDetails;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message", columnDefinition = "TEXT")
    private String failureMessage;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @PrePersist
    public void prePersist() {
        if (this.tenantId == null) {
            this.tenantId = TenantContext.getCurrentTenantId();
        }
    }
}

