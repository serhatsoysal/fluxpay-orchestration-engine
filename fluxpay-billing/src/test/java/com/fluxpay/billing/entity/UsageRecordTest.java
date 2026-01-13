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
        UsageRecord usageRecord = new UsageRecord();
        usageRecord.setSubscriptionId(UUID.randomUUID());
        usageRecord.setSubscriptionItemId(UUID.randomUUID());
        usageRecord.setMeterName("api_calls");
        usageRecord.setQuantity(BigDecimal.valueOf(100));

        usageRecord.prePersist();

        assertThat(usageRecord.getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void prePersist_ShouldSetTimestamp() {
        UsageRecord usageRecord = new UsageRecord();
        usageRecord.setSubscriptionId(UUID.randomUUID());
        usageRecord.setSubscriptionItemId(UUID.randomUUID());
        usageRecord.setMeterName("api_calls");
        usageRecord.setQuantity(BigDecimal.valueOf(100));

        usageRecord.prePersist();

        assertThat(usageRecord.getTimestamp()).isNotNull();
    }

    @Test
    void prePersist_ShouldNotOverrideExistingTimestamp() {
        Instant existingTimestamp = Instant.now().minusSeconds(3600);
        UsageRecord usageRecord = new UsageRecord();
        usageRecord.setSubscriptionId(UUID.randomUUID());
        usageRecord.setSubscriptionItemId(UUID.randomUUID());
        usageRecord.setMeterName("api_calls");
        usageRecord.setQuantity(BigDecimal.valueOf(100));
        usageRecord.setTimestamp(existingTimestamp);

        usageRecord.prePersist();

        assertThat(usageRecord.getTimestamp()).isEqualTo(existingTimestamp);
    }
}

