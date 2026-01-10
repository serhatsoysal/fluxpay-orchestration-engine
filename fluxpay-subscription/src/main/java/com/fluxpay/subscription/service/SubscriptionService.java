package com.fluxpay.subscription.service;

import com.fluxpay.common.dto.PageResponse;
import com.fluxpay.common.enums.SubscriptionStatus;
import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.security.context.TenantContext;
import com.fluxpay.subscription.entity.Subscription;
import com.fluxpay.subscription.entity.SubscriptionItem;
import com.fluxpay.subscription.repository.SubscriptionItemRepository;
import com.fluxpay.subscription.repository.SubscriptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionItemRepository subscriptionItemRepository;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            SubscriptionItemRepository subscriptionItemRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionItemRepository = subscriptionItemRepository;
    }

    public Subscription createSubscription(Subscription subscription, List<SubscriptionItem> items) {
        Subscription savedSubscription = subscriptionRepository.save(subscription);
        
        for (SubscriptionItem item : items) {
            item.setSubscriptionId(savedSubscription.getId());
            subscriptionItemRepository.save(item);
        }

        return savedSubscription;
    }

    @Transactional(readOnly = true)
    public Subscription getSubscriptionById(UUID id) {
        return subscriptionRepository.findById(id)
                .filter(s -> s.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", id));
    }

    @Transactional(readOnly = true)
    public PageResponse<Subscription> getSubscriptions(int page, int size, SubscriptionStatus status) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Pageable pageable = PageRequest.of(page, size);
        Page<Subscription> subscriptionPage = subscriptionRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        
        return new PageResponse<>(
                subscriptionPage.getContent(),
                subscriptionPage.getNumber(),
                subscriptionPage.getSize(),
                subscriptionPage.getTotalElements(),
                subscriptionPage.getTotalPages(),
                subscriptionPage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public List<Subscription> getSubscriptionsByCustomer(UUID customerId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return subscriptionRepository.findByTenantIdAndCustomerId(tenantId, customerId);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionItem> getSubscriptionItems(UUID subscriptionId) {
        return subscriptionItemRepository.findBySubscriptionId(subscriptionId);
    }

    public Subscription activateSubscription(UUID id) {
        Subscription subscription = getSubscriptionById(id);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        return subscriptionRepository.save(subscription);
    }

    public Subscription cancelSubscription(UUID id, String reason, boolean immediately) {
        Subscription subscription = getSubscriptionById(id);
        
        subscription.setCancellationReason(reason);
        subscription.setCanceledAt(Instant.now());
        
        if (immediately) {
            subscription.setStatus(SubscriptionStatus.CANCELED);
            subscription.setCancelAt(Instant.now());
        } else {
            subscription.setCancelAt(subscription.getCurrentPeriodEnd());
        }

        return subscriptionRepository.save(subscription);
    }

    public Subscription pauseSubscription(UUID id) {
        Subscription subscription = getSubscriptionById(id);
        subscription.setStatus(SubscriptionStatus.PAUSED);
        return subscriptionRepository.save(subscription);
    }

    public Subscription resumeSubscription(UUID id) {
        Subscription subscription = getSubscriptionById(id);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        return subscriptionRepository.save(subscription);
    }
}

