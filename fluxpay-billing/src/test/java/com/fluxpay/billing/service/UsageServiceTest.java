package com.fluxpay.billing.service;

import com.fluxpay.billing.entity.UsageRecord;
import com.fluxpay.billing.repository.UsageRecordRepository;
import com.fluxpay.common.enums.UsageAggregationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsageServiceTest {

    @Mock
    private UsageRecordRepository usageRecordRepository;

    @InjectMocks
    private UsageService usageService;

    @Test
    void aggregateUsage_SumType_ShouldReturnSum() {
        when(usageRecordRepository.sumQuantityBySubscriptionItemIdAndTimestampBetween(any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(150));

        BigDecimal result = usageService.aggregateUsage(
                UUID.randomUUID(),
                Instant.now().minusSeconds(3600),
                Instant.now(),
                UsageAggregationType.SUM
        );

        assertThat(result).isEqualTo(BigDecimal.valueOf(150));
    }

    @Test
    void aggregateUsage_MaxType_ShouldReturnMax() {
        when(usageRecordRepository.maxQuantityBySubscriptionItemIdAndTimestampBetween(any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(100));

        BigDecimal result = usageService.aggregateUsage(
                UUID.randomUUID(),
                Instant.now().minusSeconds(3600),
                Instant.now(),
                UsageAggregationType.MAX
        );

        assertThat(result).isEqualTo(BigDecimal.valueOf(100));
    }

    @Test
    void recordUsage_ShouldSaveUsageRecord() {
        UsageRecord record = new UsageRecord();
        record.setSubscriptionId(UUID.randomUUID());
        record.setMeterName("api_calls");
        record.setQuantity(BigDecimal.TEN);

        when(usageRecordRepository.save(any())).thenAnswer(i -> {
            UsageRecord saved = i.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        UsageRecord result = usageService.recordUsage(record);
        assertThat(result.getId()).isNotNull();
    }

    @Test
    void aggregateUsage_LastType_ShouldReturnLastRecord() {
        UsageRecord record = new UsageRecord();
        record.setQuantity(BigDecimal.valueOf(75));

        when(usageRecordRepository.findBySubscriptionItemIdAndTimestampBetweenOrderByTimestampDesc(any(), any(), any()))
                .thenReturn(List.of(record));

        BigDecimal result = usageService.aggregateUsage(
                UUID.randomUUID(),
                Instant.now().minusSeconds(3600),
                Instant.now(),
                UsageAggregationType.LAST
        );

        assertThat(result).isEqualTo(BigDecimal.valueOf(75));
    }

    @Test
    void aggregateUsage_LastType_ShouldReturnZeroWhenEmpty() {
        when(usageRecordRepository.findBySubscriptionItemIdAndTimestampBetweenOrderByTimestampDesc(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        BigDecimal result = usageService.aggregateUsage(
                UUID.randomUUID(),
                Instant.now().minusSeconds(3600),
                Instant.now(),
                UsageAggregationType.LAST
        );

        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void aggregateUsage_SumType_ShouldReturnZeroWhenNull() {
        when(usageRecordRepository.sumQuantityBySubscriptionItemIdAndTimestampBetween(any(), any(), any()))
                .thenReturn(null);

        BigDecimal result = usageService.aggregateUsage(
                UUID.randomUUID(),
                Instant.now().minusSeconds(3600),
                Instant.now(),
                UsageAggregationType.SUM
        );

        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void aggregateUsage_MaxType_ShouldReturnZeroWhenNull() {
        when(usageRecordRepository.maxQuantityBySubscriptionItemIdAndTimestampBetween(any(), any(), any()))
                .thenReturn(null);

        BigDecimal result = usageService.aggregateUsage(
                UUID.randomUUID(),
                Instant.now().minusSeconds(3600),
                Instant.now(),
                UsageAggregationType.MAX
        );

        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void getUsageRecords_ShouldReturnRecords() {
        UUID subscriptionId = UUID.randomUUID();
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();

        UsageRecord record1 = new UsageRecord();
        record1.setId(UUID.randomUUID());
        UsageRecord record2 = new UsageRecord();
        record2.setId(UUID.randomUUID());

        when(usageRecordRepository.findBySubscriptionIdAndTimestampBetween(subscriptionId, start, end))
                .thenReturn(List.of(record1, record2));

        List<UsageRecord> result = usageService.getUsageRecords(subscriptionId, start, end);

        assertThat(result).hasSize(2);
    }

    @Test
    void aggregateUsage_LastType_WithMultipleRecords_ShouldReturnFirst() {
        UsageRecord record1 = new UsageRecord();
        record1.setQuantity(BigDecimal.valueOf(100));
        UsageRecord record2 = new UsageRecord();
        record2.setQuantity(BigDecimal.valueOf(50));

        when(usageRecordRepository.findBySubscriptionItemIdAndTimestampBetweenOrderByTimestampDesc(any(), any(), any()))
                .thenReturn(List.of(record1, record2));

        BigDecimal result = usageService.aggregateUsage(
                UUID.randomUUID(),
                Instant.now().minusSeconds(3600),
                Instant.now(),
                UsageAggregationType.LAST
        );

        assertThat(result).isEqualTo(BigDecimal.valueOf(100));
    }
}

