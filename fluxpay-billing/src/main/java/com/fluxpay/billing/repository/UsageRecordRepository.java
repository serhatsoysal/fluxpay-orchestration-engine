package com.fluxpay.billing.repository;

import com.fluxpay.billing.entity.UsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface UsageRecordRepository extends JpaRepository<UsageRecord, UUID> {
    List<UsageRecord> findBySubscriptionIdAndTimestampBetween(UUID subscriptionId, Instant start, Instant end);
    
    List<UsageRecord> findBySubscriptionItemIdAndTimestampBetweenOrderByTimestampDesc(UUID subscriptionItemId, Instant start, Instant end);
    
    @Query("SELECT SUM(u.quantity) FROM UsageRecord u WHERE u.subscriptionItemId = :subscriptionItemId AND u.timestamp BETWEEN :start AND :end")
    BigDecimal sumQuantityBySubscriptionItemIdAndTimestampBetween(@Param("subscriptionItemId") UUID subscriptionItemId, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT MAX(u.quantity) FROM UsageRecord u WHERE u.subscriptionItemId = :subscriptionItemId AND u.timestamp BETWEEN :start AND :end")
    BigDecimal maxQuantityBySubscriptionItemIdAndTimestampBetween(@Param("subscriptionItemId") UUID subscriptionItemId, @Param("start") Instant start, @Param("end") Instant end);
}

