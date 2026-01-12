package com.fluxpay.notification.entity;

import com.fluxpay.common.entity.BaseEntity;
import com.fluxpay.common.enums.WebhookEventType;
import com.fluxpay.security.context.TenantContext;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "webhook_endpoints")
@Getter
@Setter
public class WebhookEndpoint extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 500)
    private String url;

    @Type(JsonBinaryType.class)
    @Column(name = "enabled_events", nullable = false, columnDefinition = "jsonb")
    private List<WebhookEventType> enabledEvents;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(length = 500)
    private String secret;

    @PrePersist
    public void prePersist() {
        if (this.tenantId == null) {
            this.tenantId = TenantContext.getCurrentTenantId();
        }
    }
}

