package com.fluxpay.billing.entity;

import com.fluxpay.security.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UsageRecordTest {

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void prePersist_ShouldSetTenantId() {
        UsageRecord record = new UsageRecord();
        record.setSubscriptionId(UUID.randomUUID());
        record.setSubscriptionItemId(UUID.randomUUID());
        record.setMeterName("api_calls");
        record.setQuantity(BigDecimal.valueOf(100));

        record.prePersist();

        assertThat(record.getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void prePersist_ShouldSetTimestamp() {
        UsageRecord record = new UsageRecord();
        record.setSubscriptionId(UUID.randomUUID());
        record.setSubscriptionItemId(UUID.randomUUID());
        record.setMeterName("api_calls");
        record.setQuantity(BigDecimal.valueOf(100));

        record.prePersist();

        assertThat(record.getTimestamp()).isNotNull();
    }

    @Test
    void prePersist_ShouldNotOverrideExistingTimestamp() {
        Instant existingTimestamp = Instant.now().minusSeconds(3600);
        UsageRecord record = new UsageRecord();
        record.setSubscriptionId(UUID.randomUUID());
        record.setSubscriptionItemId(UUID.randomUUID());
        record.setMeterName("api_calls");
        record.setQuantity(BigDecimal.valueOf(100));
        record.setTimestamp(existingTimestamp);

        record.prePersist();

        assertThat(record.getTimestamp()).isEqualTo(existingTimestamp);
    }
}

