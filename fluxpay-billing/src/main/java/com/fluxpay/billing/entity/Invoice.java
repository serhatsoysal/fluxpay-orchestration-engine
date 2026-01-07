package com.fluxpay.billing.entity;

import com.fluxpay.common.entity.BaseEntity;
import com.fluxpay.common.enums.InvoiceStatus;
import com.fluxpay.security.context.TenantContext;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "invoices", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "invoice_number"})
})
@Getter
@Setter
public class Invoice extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "subscription_id")
    private UUID subscriptionId;

    @Column(name = "invoice_number", nullable = false, length = 50)
    private String invoiceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(nullable = false)
    private Long subtotal;

    @Column(nullable = false)
    private Long tax = 0L;

    @Column(nullable = false)
    private Long total;

    @Column(name = "amount_due", nullable = false)
    private Long amountDue;

    @Column(name = "amount_paid", nullable = false)
    private Long amountPaid = 0L;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "period_start")
    private Instant periodStart;

    @Column(name = "period_end")
    private Instant periodEnd;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "next_payment_attempt")
    private Instant nextPaymentAttempt;

    @Column(name = "pdf_url", columnDefinition = "TEXT")
    private String pdfUrl;

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

