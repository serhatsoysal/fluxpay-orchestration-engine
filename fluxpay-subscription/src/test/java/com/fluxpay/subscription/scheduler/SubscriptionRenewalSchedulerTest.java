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

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void processTrialExpirations_ShouldSkipActiveTrials() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.TRIALING);
        subscription.setTrialEnd(Instant.now().plus(1, ChronoUnit.DAYS));

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));

        scheduler.processTrialExpirations();

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void processTrialExpirations_ShouldSkipDeletedSubscriptions() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.TRIALING);
        subscription.setTrialEnd(Instant.now().minus(1, ChronoUnit.DAYS));
        subscription.setDeletedAt(Instant.now());

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));

        scheduler.processTrialExpirations();

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void processSubscriptionRenewals_ShouldSkipWhenPeriodNotEnded() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodEnd(Instant.now().plus(2, ChronoUnit.DAYS));

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));

        scheduler.processSubscriptionRenewals();

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void processSubscriptionRenewals_ShouldSkipDeletedSubscriptions() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodEnd(Instant.now().minus(1, ChronoUnit.HOURS));
        subscription.setDeletedAt(Instant.now());

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));

        scheduler.processSubscriptionRenewals();

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void processCanceledSubscriptions_ShouldSkipWhenCancelAtNotPassed() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCancelAt(Instant.now().plus(1, ChronoUnit.DAYS));

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));

        scheduler.processCanceledSubscriptions();

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void processCanceledSubscriptions_ShouldSkipAlreadyCanceled() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscription.setCancelAt(Instant.now().minus(1, ChronoUnit.DAYS));

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));

        scheduler.processCanceledSubscriptions();

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void processTrialExpirations_ShouldSkipWhenTrialEndIsNull() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.TRIALING);
        subscription.setTrialEnd(null);

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));

        scheduler.processTrialExpirations();

        verify(subscriptionRepository, never()).save(any());
    }


    @Test
    void processSubscriptionRenewals_ShouldCalculatePeriodCorrectly() {
        Instant oldPeriodEnd = Instant.now().minus(1, ChronoUnit.HOURS);
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodEnd(oldPeriodEnd);

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        scheduler.processSubscriptionRenewals();

        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void processSubscriptionRenewals_ShouldRenewWhenCurrentPeriodEndExactlyTomorrow() {
        Instant tomorrow = Instant.now().plus(1, ChronoUnit.DAYS);
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodEnd(tomorrow);

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        scheduler.processSubscriptionRenewals();

        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void processCanceledSubscriptions_ShouldSkipWhenCancelAtIsNull() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCancelAt(null);

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));

        scheduler.processCanceledSubscriptions();

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void processCanceledSubscriptions_ShouldSkipDeletedSubscriptions() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCancelAt(Instant.now().minus(1, ChronoUnit.DAYS));
        subscription.setDeletedAt(Instant.now());

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));

        scheduler.processCanceledSubscriptions();

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void processTrialExpirations_ShouldSkipNonTrialingSubscriptions() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setTrialEnd(Instant.now().minus(1, ChronoUnit.DAYS));

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));

        scheduler.processTrialExpirations();

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void processSubscriptionRenewals_ShouldSkipNonActiveSubscriptions() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscription.setCurrentPeriodEnd(Instant.now().minus(1, ChronoUnit.HOURS));

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));

        scheduler.processSubscriptionRenewals();

        verify(subscriptionRepository, never()).save(any());
    }
}

