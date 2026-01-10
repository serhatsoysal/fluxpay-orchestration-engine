package com.fluxpay.subscription.service;

import com.fluxpay.common.dto.PageResponse;
import com.fluxpay.common.enums.SubscriptionStatus;
import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.security.context.TenantContext;
import com.fluxpay.subscription.entity.Subscription;
import com.fluxpay.subscription.entity.SubscriptionItem;
import com.fluxpay.subscription.repository.SubscriptionItemRepository;
import com.fluxpay.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionItemRepository subscriptionItemRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private UUID tenantId;
    private UUID customerId;
    private Subscription subscription;
    private SubscriptionItem item;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);

        subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setTenantId(tenantId);
        subscription.setCustomerId(customerId);
        subscription.setStatus(SubscriptionStatus.INCOMPLETE);
        subscription.setCurrentPeriodStart(Instant.now());
        subscription.setCurrentPeriodEnd(Instant.now().plusSeconds(86400));
        subscription.setBillingCycleAnchor(Instant.now());

        item = new SubscriptionItem();
        item.setId(UUID.randomUUID());
        item.setPriceId(UUID.randomUUID());
        item.setQuantity(1);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createSubscription_Success() {
        List<SubscriptionItem> items = List.of(item);
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);
        when(subscriptionItemRepository.save(any(SubscriptionItem.class))).thenReturn(item);

        Subscription result = subscriptionService.createSubscription(subscription, items);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(subscription.getId());
        verify(subscriptionRepository).save(subscription);
        verify(subscriptionItemRepository).save(any(SubscriptionItem.class));
    }

    @Test
    void createSubscription_SetsSubscriptionId_OnItems() {
        List<SubscriptionItem> items = List.of(item);
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);
        when(subscriptionItemRepository.save(any(SubscriptionItem.class))).thenReturn(item);

        subscriptionService.createSubscription(subscription, items);

        assertThat(item.getSubscriptionId()).isEqualTo(subscription.getId());
        verify(subscriptionItemRepository).save(item);
    }

    @Test
    void getSubscriptionById_Success() {
        UUID id = subscription.getId();
        when(subscriptionRepository.findById(id)).thenReturn(Optional.of(subscription));

        Subscription result = subscriptionService.getSubscriptionById(id);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    void getSubscriptionById_ThrowsException_WhenNotFound() {
        UUID id = UUID.randomUUID();
        when(subscriptionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.getSubscriptionById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getSubscriptionById_ThrowsException_WhenDeleted() {
        subscription.softDelete();
        UUID id = subscription.getId();
        when(subscriptionRepository.findById(id)).thenReturn(Optional.of(subscription));

        assertThatThrownBy(() -> subscriptionService.getSubscriptionById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getSubscriptions_Success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Subscription> subscriptionPage = new PageImpl<>(
                List.of(subscription),
                pageable,
                1L
        );

        when(subscriptionRepository.findByTenantIdAndStatus(eq(tenantId), eq(null), eq(pageable)))
                .thenReturn(subscriptionPage);

        PageResponse<Subscription> result = subscriptionService.getSubscriptions(0, 20, null);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }

    @Test
    void getSubscriptions_WithStatus() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Subscription> subscriptionPage = new PageImpl<>(
                List.of(subscription),
                pageable,
                1L
        );

        when(subscriptionRepository.findByTenantIdAndStatus(eq(tenantId), eq(SubscriptionStatus.ACTIVE), eq(pageable)))
                .thenReturn(subscriptionPage);

        PageResponse<Subscription> result = subscriptionService.getSubscriptions(0, 20, SubscriptionStatus.ACTIVE);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getSubscriptionsByCustomer_Success() {
        List<Subscription> subscriptions = List.of(subscription);
        when(subscriptionRepository.findByTenantIdAndCustomerId(tenantId, customerId))
                .thenReturn(subscriptions);

        List<Subscription> result = subscriptionService.getSubscriptionsByCustomer(customerId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerId()).isEqualTo(customerId);
    }

    @Test
    void getSubscriptionItems_Success() {
        UUID subscriptionId = subscription.getId();
        List<SubscriptionItem> items = List.of(item);
        when(subscriptionItemRepository.findBySubscriptionId(subscriptionId)).thenReturn(items);

        List<SubscriptionItem> result = subscriptionService.getSubscriptionItems(subscriptionId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(item.getId());
    }

    @Test
    void getSubscriptionItems_ReturnsEmptyList() {
        UUID subscriptionId = subscription.getId();
        when(subscriptionItemRepository.findBySubscriptionId(subscriptionId)).thenReturn(Collections.emptyList());

        List<SubscriptionItem> result = subscriptionService.getSubscriptionItems(subscriptionId);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void activateSubscription_Success() {
        UUID id = subscription.getId();
        when(subscriptionRepository.findById(id)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);

        Subscription result = subscriptionService.activateSubscription(id);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void cancelSubscription_Success_Immediately() {
        UUID id = subscription.getId();
        when(subscriptionRepository.findById(id)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);

        Subscription result = subscriptionService.cancelSubscription(id, "Test reason", true);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(result.getCancellationReason()).isEqualTo("Test reason");
        assertThat(result.getCanceledAt()).isNotNull();
        assertThat(result.getCancelAt()).isNotNull();
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void cancelSubscription_Success_AtPeriodEnd() {
        UUID id = subscription.getId();
        Instant periodEnd = Instant.now().plusSeconds(86400);
        subscription.setCurrentPeriodEnd(periodEnd);
        when(subscriptionRepository.findById(id)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);

        Subscription result = subscriptionService.cancelSubscription(id, "Test reason", false);

        assertThat(result).isNotNull();
        assertThat(result.getCancellationReason()).isEqualTo("Test reason");
        assertThat(result.getCanceledAt()).isNotNull();
        assertThat(result.getCancelAt()).isEqualTo(periodEnd);
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void pauseSubscription_Success() {
        UUID id = subscription.getId();
        when(subscriptionRepository.findById(id)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);

        Subscription result = subscriptionService.pauseSubscription(id);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.PAUSED);
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void resumeSubscription_Success() {
        UUID id = subscription.getId();
        subscription.setStatus(SubscriptionStatus.PAUSED);
        when(subscriptionRepository.findById(id)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);

        Subscription result = subscriptionService.resumeSubscription(id);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(subscriptionRepository).save(subscription);
    }
}
