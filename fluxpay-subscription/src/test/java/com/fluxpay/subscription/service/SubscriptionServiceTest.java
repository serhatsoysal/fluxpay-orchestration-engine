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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionItemRepository subscriptionItemRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private Subscription subscription;
    private UUID subscriptionId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        subscriptionId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);
        
        subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setTenantId(tenantId);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodEnd(Instant.now().plusSeconds(86400));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createSubscription_ShouldSucceed() {
        SubscriptionItem item = new SubscriptionItem();
        item.setId(UUID.randomUUID());

        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);
        when(subscriptionItemRepository.save(any(SubscriptionItem.class))).thenReturn(item);

        Subscription result = subscriptionService.createSubscription(subscription, List.of(item));

        assertThat(result).isEqualTo(subscription);
        verify(subscriptionItemRepository).save(item);
    }

    @Test
    void getSubscriptionById_ShouldReturnSubscription() {
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        Subscription result = subscriptionService.getSubscriptionById(subscriptionId);

        assertThat(result).isEqualTo(subscription);
    }

    @Test
    void getSubscriptionById_ShouldThrowException_WhenNotFound() {
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.getSubscriptionById(subscriptionId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getSubscriptions_ShouldReturnPageResponse() {
        when(subscriptionRepository.findByTenantIdAndStatus(eq(tenantId), eq(SubscriptionStatus.ACTIVE), any()))
                .thenReturn(new PageImpl<>(List.of(subscription), PageRequest.of(0, 10), 1));

        PageResponse<Subscription> result = subscriptionService.getSubscriptions(0, 10, SubscriptionStatus.ACTIVE);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(subscription);
    }

    @Test
    void activateSubscription_ShouldSucceed() {
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);

        Subscription result = subscriptionService.activateSubscription(subscriptionId);

        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void cancelSubscription_Immediately_ShouldSucceed() {
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);

        Subscription result = subscriptionService.cancelSubscription(subscriptionId, "Test reason", true);

        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(result.getCanceledAt()).isNotNull();
    }

    @Test
    void cancelSubscription_AtPeriodEnd_ShouldSucceed() {
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);

        Subscription result = subscriptionService.cancelSubscription(subscriptionId, "Test reason", false);

        assertThat(result.getCancelAt()).isEqualTo(subscription.getCurrentPeriodEnd());
    }

    @Test
    void pauseSubscription_ShouldSucceed() {
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);

        Subscription result = subscriptionService.pauseSubscription(subscriptionId);

        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.PAUSED);
    }

    @Test
    void resumeSubscription_ShouldSucceed() {
        subscription.setStatus(SubscriptionStatus.PAUSED);
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);

        Subscription result = subscriptionService.resumeSubscription(subscriptionId);

        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }
}

