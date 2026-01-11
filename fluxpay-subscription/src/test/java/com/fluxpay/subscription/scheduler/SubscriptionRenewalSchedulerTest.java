package com.fluxpay.subscription.scheduler;

import com.fluxpay.common.enums.SubscriptionStatus;
import com.fluxpay.subscription.entity.Subscription;
import com.fluxpay.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionRenewalSchedulerTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionRenewalScheduler scheduler;

    @Test
    void processTrialExpirations_ShouldUpdateExpiredTrials() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.TRIALING);
        subscription.setTrialEnd(Instant.now().minus(1, ChronoUnit.DAYS));

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        scheduler.processTrialExpirations();

        verify(subscriptionRepository).save(argThat(s -> s.getStatus() == SubscriptionStatus.ACTIVE));
    }

    @Test
    void processSubscriptionRenewals_ShouldRenewActiveSubscriptions() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodEnd(Instant.now().minus(1, ChronoUnit.HOURS));

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        scheduler.processSubscriptionRenewals();

        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void processCanceledSubscriptions_ShouldCancelScheduledSubscriptions() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCancelAt(Instant.now().minus(1, ChronoUnit.DAYS));

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        scheduler.processCanceledSubscriptions();

        verify(subscriptionRepository).save(argThat(s -> s.getStatus() == SubscriptionStatus.CANCELED));
    }
}

