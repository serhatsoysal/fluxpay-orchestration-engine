package com.fluxpay.subscription.repository;

import com.fluxpay.common.enums.SubscriptionStatus;
import com.fluxpay.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    List<Subscription> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId);

    List<Subscription> findByStatus(SubscriptionStatus status);

    List<Subscription> findByStatusInAndCurrentPeriodEndBefore(List<SubscriptionStatus> statuses, Instant date);
}

