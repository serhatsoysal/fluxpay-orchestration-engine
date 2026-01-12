package com.fluxpay.billing.service;

import com.fluxpay.billing.entity.UsageRecord;
import com.fluxpay.billing.repository.UsageRecordRepository;
import com.fluxpay.common.enums.UsageAggregationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UsageService {

    private final UsageRecordRepository usageRecordRepository;

    public UsageService(UsageRecordRepository usageRecordRepository) {
        this.usageRecordRepository = usageRecordRepository;
    }

    public UsageRecord recordUsage(UsageRecord usageRecord) {
        return usageRecordRepository.save(usageRecord);
    }

    public List<UsageRecord> getUsageRecords(UUID subscriptionId, Instant start, Instant end) {
        return usageRecordRepository.findBySubscriptionIdAndTimestampBetween(subscriptionId, start, end);
    }

    public BigDecimal aggregateUsage(UUID subscriptionItemId, Instant start, Instant end, UsageAggregationType aggregationType) {
        return switch (aggregationType) {
            case SUM -> {
                BigDecimal sum = usageRecordRepository.sumQuantityBySubscriptionItemIdAndTimestampBetween(subscriptionItemId, start, end);
                yield sum != null ? sum : BigDecimal.ZERO;
            }
            case MAX -> {
                BigDecimal max = usageRecordRepository.maxQuantityBySubscriptionItemIdAndTimestampBetween(subscriptionItemId, start, end);
                yield max != null ? max : BigDecimal.ZERO;
            }
            case LAST -> {
                List<UsageRecord> records = usageRecordRepository.findBySubscriptionItemIdAndTimestampBetweenOrderByTimestampDesc(subscriptionItemId, start, end);
                if (records.isEmpty()) {
                    yield BigDecimal.ZERO;
                }
                yield records.get(0).getQuantity();
            }
        };
    }
}

