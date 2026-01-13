package com.fluxpay.billing.repository;

import com.fluxpay.billing.entity.UsageRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Transactional
class UsageRecordRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private UsageRecordRepository usageRecordRepository;

    private UUID subscriptionId1;
    private UUID subscriptionId2;
    private UUID subscriptionItemId1;
    private UUID subscriptionItemId2;
    private UsageRecord record1;
    private UsageRecord record2;
    private UsageRecord record3;

    @BeforeEach
    void setUp() {
        usageRecordRepository.deleteAll();

        subscriptionId1 = UUID.randomUUID();
        subscriptionId2 = UUID.randomUUID();
        subscriptionItemId1 = UUID.randomUUID();
        subscriptionItemId2 = UUID.randomUUID();

        Instant now = Instant.now();
        Instant start = now.minusSeconds(3600);

        record1 = createUsageRecord(subscriptionId1, subscriptionItemId1, BigDecimal.valueOf(10), start);
        record2 = createUsageRecord(subscriptionId1, subscriptionItemId1, BigDecimal.valueOf(20), now);
        record3 = createUsageRecord(subscriptionId1, subscriptionItemId2, BigDecimal.valueOf(15), now);
        UsageRecord record4 = createUsageRecord(subscriptionId2, subscriptionItemId1, BigDecimal.valueOf(5), now);

        usageRecordRepository.saveAll(List.of(record1, record2, record3, record4));
    }

    private UsageRecord createUsageRecord(UUID subscriptionId, UUID subscriptionItemId, BigDecimal quantity, Instant usageTimestamp) {
        UsageRecord record = new UsageRecord();
        record.setSubscriptionId(subscriptionId);
        record.setSubscriptionItemId(subscriptionItemId);
        record.setQuantity(quantity);
        record.setTimestamp(usageTimestamp);
        return record;
    }

    @Test
    void findBySubscriptionIdAndTimestampBetween_ShouldReturnRecordsInRange() {
        Instant start = Instant.now().minusSeconds(7200);
        Instant end = Instant.now().plusSeconds(7200);

        List<UsageRecord> records = usageRecordRepository.findBySubscriptionIdAndTimestampBetween(subscriptionId1, start, end);

        assertThat(records)
                .hasSize(3)
                .allMatch(r -> r.getSubscriptionId().equals(subscriptionId1))
                .allMatch(r -> !r.getTimestamp().isBefore(start) && !r.getTimestamp().isAfter(end));
    }

    @Test
    void findBySubscriptionItemIdAndTimestampBetweenOrderByTimestampDesc_ShouldReturnOrdered() {
        Instant start = Instant.now().minusSeconds(7200);
        Instant end = Instant.now().plusSeconds(7200);

        List<UsageRecord> records = usageRecordRepository.findBySubscriptionItemIdAndTimestampBetweenOrderByTimestampDesc(
                subscriptionItemId1, start, end
        );

        assertThat(records)
                .hasSize(2)
                .allMatch(r -> r.getSubscriptionItemId().equals(subscriptionItemId1));
        for (int i = 0; i < records.size() - 1; i++) {
            assertThat(records.get(i).getTimestamp())
                    .isAfterOrEqualTo(records.get(i + 1).getTimestamp());
        }
    }

    @Test
    void sumQuantityBySubscriptionItemIdAndTimestampBetween_ShouldSumQuantities() {
        Instant start = Instant.now().minusSeconds(7200);
        Instant end = Instant.now().plusSeconds(7200);

        BigDecimal sum = usageRecordRepository.sumQuantityBySubscriptionItemIdAndTimestampBetween(
                subscriptionItemId1, start, end
        );

        assertThat(sum).isEqualByComparingTo(BigDecimal.valueOf(30));
    }

    @Test
    void sumQuantityBySubscriptionItemIdAndTimestampBetween_WithNoRecords_ShouldReturnZero() {
        Instant start = Instant.now().plusSeconds(7200);
        Instant end = Instant.now().plusSeconds(10800);

        BigDecimal sum = usageRecordRepository.sumQuantityBySubscriptionItemIdAndTimestampBetween(
                subscriptionItemId1, start, end
        );

        assertThat(sum).isNull();
    }

    @Test
    void maxQuantityBySubscriptionItemIdAndTimestampBetween_ShouldReturnMax() {
        Instant start = Instant.now().minusSeconds(7200);
        Instant end = Instant.now().plusSeconds(7200);

        BigDecimal max = usageRecordRepository.maxQuantityBySubscriptionItemIdAndTimestampBetween(
                subscriptionItemId1, start, end
        );

        assertThat(max).isEqualByComparingTo(BigDecimal.valueOf(20));
    }

    @Test
    void maxQuantityBySubscriptionItemIdAndTimestampBetween_WithNoRecords_ShouldReturnNull() {
        Instant start = Instant.now().plusSeconds(7200);
        Instant end = Instant.now().plusSeconds(10800);

        BigDecimal max = usageRecordRepository.maxQuantityBySubscriptionItemIdAndTimestampBetween(
                subscriptionItemId1, start, end
        );

        assertThat(max).isNull();
    }
}

