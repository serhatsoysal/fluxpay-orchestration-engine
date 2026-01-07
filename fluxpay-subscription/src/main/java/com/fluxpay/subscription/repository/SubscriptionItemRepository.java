package com.fluxpay.subscription.repository;

import com.fluxpay.subscription.entity.SubscriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionItemRepository extends JpaRepository<SubscriptionItem, UUID> {

    List<SubscriptionItem> findBySubscriptionId(UUID subscriptionId);
}

