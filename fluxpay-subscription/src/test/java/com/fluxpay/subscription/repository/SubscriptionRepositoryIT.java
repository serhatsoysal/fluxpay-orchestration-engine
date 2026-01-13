package com.fluxpay.subscription.repository;

import com.fluxpay.common.enums.SubscriptionStatus;
import com.fluxpay.subscription.entity.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Transactional
class SubscriptionRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    private UUID tenantId1;
    private UUID tenantId2;
    private UUID customerId1;
    private UUID customerId2;
    private Subscription subscription1;
    private Subscription subscription2;
    private Subscription subscription3;

    @BeforeEach
    void setUp() {
        subscriptionRepository.deleteAll();

        tenantId1 = UUID.randomUUID();
        tenantId2 = UUID.randomUUID();
        customerId1 = UUID.randomUUID();
        customerId2 = UUID.randomUUID();

        subscription1 = createSubscription(tenantId1, customerId1, SubscriptionStatus.ACTIVE);
        subscription2 = createSubscription(tenantId1, customerId1, SubscriptionStatus.PAUSED);
        subscription3 = createSubscription(tenantId1, customerId2, SubscriptionStatus.ACTIVE);
        Subscription subscription4 = createSubscription(tenantId2, customerId1, SubscriptionStatus.ACTIVE);

        subscriptionRepository.saveAll(List.of(subscription1, subscription2, subscription3, subscription4));
    }

    private Subscription createSubscription(UUID tenantId, UUID customerId, SubscriptionStatus status) {
        Subscription subscription = new Subscription();
        subscription.setTenantId(tenantId);
        subscription.setCustomerId(customerId);
        subscription.setStatus(status);
        subscription.setCurrentPeriodStart(Instant.now());
        subscription.setCurrentPeriodEnd(Instant.now().plusSeconds(2592000));
        subscription.setCreatedAt(Instant.now());
        return subscription;
    }

    @Test
    void findByTenantIdAndCustomerId_ShouldReturnOnlyTenantSubscriptions() {
        List<Subscription> subscriptions = subscriptionRepository.findByTenantIdAndCustomerId(tenantId1, customerId1);

        assertThat(subscriptions).hasSize(2);
        assertThat(subscriptions).allMatch(s -> s.getTenantId().equals(tenantId1));
        assertThat(subscriptions).allMatch(s -> s.getCustomerId().equals(customerId1));
    }

    @Test
    void findByStatus_ShouldReturnSubscriptionsByStatus() {
        List<Subscription> active = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE);

        assertThat(active)
                .hasSize(3)
                .allMatch(s -> s.getStatus().equals(SubscriptionStatus.ACTIVE));
    }

    @Test
    void findByStatusInAndCurrentPeriodEndBefore_ShouldReturnExpired() {
        Instant pastDate = Instant.now().minusSeconds(86400);
        subscription1.setCurrentPeriodEnd(pastDate);
        subscription3.setCurrentPeriodEnd(pastDate);
        subscriptionRepository.saveAll(List.of(subscription1, subscription3));

        List<Subscription> expired = subscriptionRepository.findByStatusInAndCurrentPeriodEndBefore(
                List.of(SubscriptionStatus.ACTIVE), Instant.now()
        );

        assertThat(expired)
                .hasSize(2);
        assertThat(expired).allMatch(s -> s.getCurrentPeriodEnd().isBefore(Instant.now()));
    }

    @Test
    void findByTenantIdAndStatus_WithPagination_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 2);

        Page<Subscription> result = subscriptionRepository.findByTenantIdAndStatus(tenantId1, SubscriptionStatus.ACTIVE, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(result.getContent()).allMatch(s -> s.getTenantId().equals(tenantId1));
        assertThat(result.getContent()).allMatch(s -> s.getStatus().equals(SubscriptionStatus.ACTIVE));
    }

    @Test
    void findByTenantIdAndStatus_WithNullStatus_ShouldReturnAll() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<Subscription> result = subscriptionRepository.findByTenantIdAndStatus(tenantId1, null, pageable);

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent()).allMatch(s -> s.getTenantId().equals(tenantId1));
    }

    @Test
    void findByTenantIdAndStatus_ShouldRespectTenantIsolation() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<Subscription> tenant1Subs = subscriptionRepository.findByTenantIdAndStatus(tenantId1, null, pageable);
        Page<Subscription> tenant2Subs = subscriptionRepository.findByTenantIdAndStatus(tenantId2, null, pageable);

        assertThat(tenant1Subs.getContent()).hasSize(3);
        assertThat(tenant2Subs.getContent()).hasSize(1);
        assertThat(tenant1Subs.getContent()).noneMatch(s -> s.getTenantId().equals(tenantId2));
        assertThat(tenant2Subs.getContent()).noneMatch(s -> s.getTenantId().equals(tenantId1));
    }

    @Test
    void softDelete_ShouldExcludeDeletedSubscriptions() {
        subscription1.setDeletedAt(Instant.now());
        subscriptionRepository.save(subscription1);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Subscription> result = subscriptionRepository.findByTenantIdAndStatus(tenantId1, null, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).noneMatch(s -> s.getId().equals(subscription1.getId()));
    }
}

