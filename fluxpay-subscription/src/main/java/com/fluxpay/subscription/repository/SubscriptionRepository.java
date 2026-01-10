package com.fluxpay.subscription.repository;

import com.fluxpay.common.enums.SubscriptionStatus;
import com.fluxpay.subscription.entity.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    List<Subscription> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId);

    List<Subscription> findByStatus(SubscriptionStatus status);

    List<Subscription> findByStatusInAndCurrentPeriodEndBefore(List<SubscriptionStatus> statuses, Instant date);

    @Query("SELECT s FROM Subscription s WHERE s.tenantId = :tenantId AND s.deletedAt IS NULL AND (:status IS NULL OR s.status = :status)")
    Page<Subscription> findByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") SubscriptionStatus status, Pageable pageable);
}

