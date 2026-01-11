package com.fluxpay.subscription.scheduler;

import com.fluxpay.common.enums.SubscriptionStatus;
import com.fluxpay.subscription.entity.Subscription;
import com.fluxpay.subscription.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class SubscriptionRenewalScheduler {

    private final SubscriptionRepository subscriptionRepository;

    @Value("${SUBSCRIPTION_DEFAULT_PERIOD_DAYS:30}")
    private int defaultPeriodDays;

    public SubscriptionRenewalScheduler(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Scheduled(cron = "${SUBSCRIPTION_TRIAL_EXPIRATION_CRON:0 0 2 * * ?}")
    @Transactional
    public void processTrialExpirations() {
        Instant now = Instant.now();
        List<Subscription> trialSubscriptions = subscriptionRepository.findAll().stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.TRIALING)
                .filter(s -> s.getTrialEnd() != null && s.getTrialEnd().isBefore(now))
                .filter(s -> s.getDeletedAt() == null)
                .toList();

        for (Subscription subscription : trialSubscriptions) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscriptionRepository.save(subscription);
        }
    }

    @Scheduled(cron = "${SUBSCRIPTION_RENEWAL_CRON:0 0 3 * * ?}")
    @Transactional
    public void processSubscriptionRenewals() {
        Instant now = Instant.now();
        Instant tomorrow = now.plus(1, ChronoUnit.DAYS);

        List<Subscription> renewalSubscriptions = subscriptionRepository.findAll().stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .filter(s -> s.getCurrentPeriodEnd().isBefore(tomorrow))
                .filter(s -> s.getDeletedAt() == null)
                .toList();

        for (Subscription subscription : renewalSubscriptions) {
            Instant newPeriodStart = subscription.getCurrentPeriodEnd();
            Instant newPeriodEnd = newPeriodStart.plus(defaultPeriodDays, ChronoUnit.DAYS);

            subscription.setCurrentPeriodStart(newPeriodStart);
            subscription.setCurrentPeriodEnd(newPeriodEnd);
            subscriptionRepository.save(subscription);
        }
    }

    @Scheduled(cron = "${SUBSCRIPTION_CANCELLATION_CRON:0 0 4 * * ?}")
    @Transactional
    public void processCanceledSubscriptions() {
        Instant now = Instant.now();

        List<Subscription> canceledSubscriptions = subscriptionRepository.findAll().stream()
                .filter(s -> s.getStatus() != SubscriptionStatus.CANCELED)
                .filter(s -> s.getCancelAt() != null && s.getCancelAt().isBefore(now))
                .filter(s -> s.getDeletedAt() == null)
                .toList();

        for (Subscription subscription : canceledSubscriptions) {
            subscription.setStatus(SubscriptionStatus.CANCELED);
            subscriptionRepository.save(subscription);
        }
    }
}

