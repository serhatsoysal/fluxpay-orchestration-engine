package com.fluxpay.subscription.service;

import com.fluxpay.common.enums.SubscriptionStatus;
import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.security.context.TenantContext;
import com.fluxpay.subscription.entity.Customer;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private Customer customer;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);
        
        customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setEmail("test@example.com");

        subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setCustomerId(customer.getId());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getSubscriptionById_Success() {
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));

        Subscription result = subscriptionService.getSubscriptionById(subscription.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(subscription.getId());
    }

    @Test
    void getSubscriptionById_ThrowsException_WhenNotFound() {
        UUID id = UUID.randomUUID();
        when(subscriptionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.getSubscriptionById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getSubscriptionsByCustomer_Success() {
        List<Subscription> subscriptions = Arrays.asList(subscription);
        when(subscriptionRepository.findByTenantIdAndCustomerId(tenantId, customer.getId())).thenReturn(subscriptions);

        List<Subscription> result = subscriptionService.getSubscriptionsByCustomer(customer.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerId()).isEqualTo(customer.getId());
    }
}

