package com.fluxpay.subscription.entity;

import com.fluxpay.common.entity.BaseEntity;
import com.fluxpay.security.context.TenantContext;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "customers", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "email"})
})
@Getter
@Setter
public class Customer extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String email;

    @Column
    private String name;

    @Type(JsonBinaryType.class)
    @Column(name = "billing_address", columnDefinition = "jsonb")
    private Map<String, Object> billingAddress;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(name = "default_payment_method_id")
    private String defaultPaymentMethodId;

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

