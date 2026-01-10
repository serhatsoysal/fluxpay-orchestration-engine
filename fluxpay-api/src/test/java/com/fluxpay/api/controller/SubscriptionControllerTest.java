package com.fluxpay.api.controller;

import com.fluxpay.api.controller.SubscriptionRequest;
import com.fluxpay.common.dto.PageResponse;
import com.fluxpay.common.enums.SubscriptionStatus;
import com.fluxpay.subscription.entity.Customer;
import com.fluxpay.subscription.entity.Subscription;
import com.fluxpay.subscription.entity.SubscriptionItem;
import com.fluxpay.subscription.service.CustomerService;
import com.fluxpay.subscription.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private SubscriptionController subscriptionController;

    private Subscription subscription;
    private Customer customer;
    private SubscriptionItem item;
    private SubscriptionRequest request;
    private UUID subscriptionId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        subscriptionId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        customer = new Customer();
        customer.setId(customerId);
        customer.setEmail("customer@example.com");
        customer.setName("Test Customer");

        subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setCustomerId(customerId);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(Instant.now());
        subscription.setCurrentPeriodEnd(Instant.now().plusSeconds(86400));
        subscription.setBillingCycleAnchor(Instant.now());

        item = new SubscriptionItem();
        item.setId(UUID.randomUUID());
        item.setSubscriptionId(subscriptionId);
        item.setPriceId(UUID.randomUUID());
        item.setQuantity(1);

        request = new SubscriptionRequest();
        request.setSubscription(subscription);
        request.setItems(List.of(item));
    }

    @Test
    void getSubscriptions_Success() {
        PageResponse<Subscription> pageResponse = new PageResponse<>(
                List.of(subscription),
                0,
                20,
                1L,
                1,
                true
        );

        when(subscriptionService.getSubscriptions(0, 20, null)).thenReturn(pageResponse);

        ResponseEntity<PageResponse<Subscription>> response = subscriptionController.getSubscriptions(0, 20, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).getId()).isEqualTo(subscriptionId);
    }

    @Test
    void getSubscriptions_WithStatus() {
        PageResponse<Subscription> pageResponse = new PageResponse<>(
                List.of(subscription),
                0,
                20,
                1L,
                1,
                true
        );

        when(subscriptionService.getSubscriptions(0, 20, SubscriptionStatus.ACTIVE))
                .thenReturn(pageResponse);

        ResponseEntity<PageResponse<Subscription>> response = subscriptionController.getSubscriptions(0, 20, SubscriptionStatus.ACTIVE);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
    }

    @Test
    void createSubscription_Success_WithCustomerId() {
        request.setCustomerId(customerId);
        when(customerService.getCustomerById(customerId)).thenReturn(customer);
        when(subscriptionService.createSubscription(any(Subscription.class), anyList()))
                .thenReturn(subscription);

        ResponseEntity<Subscription> response = subscriptionController.createSubscription(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(subscriptionId);
        verify(customerService).getCustomerById(customerId);
        verify(subscriptionService).createSubscription(any(Subscription.class), eq(List.of(item)));
    }

    @Test
    void createSubscription_Success_WithCustomerEmail() {
        request.setCustomerId(null);
        request.setCustomerEmail("new@example.com");
        request.setCustomerName("New Customer");

        Customer newCustomer = new Customer();
        newCustomer.setId(UUID.randomUUID());
        newCustomer.setEmail("new@example.com");
        newCustomer.setName("New Customer");

        when(customerService.createCustomer(any(Customer.class))).thenReturn(newCustomer);
        when(subscriptionService.createSubscription(any(Subscription.class), anyList()))
                .thenReturn(subscription);

        ResponseEntity<Subscription> response = subscriptionController.createSubscription(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        verify(customerService).createCustomer(any(Customer.class));
        verify(subscriptionService).createSubscription(any(Subscription.class), eq(List.of(item)));
    }

    @Test
    void createSubscription_Success_CreatesCustomer_WhenCustomerIdNotFound() {
        request.setCustomerId(customerId);
        request.setCustomerEmail("new@example.com");
        request.setCustomerName("New Customer");

        Customer newCustomer = new Customer();
        newCustomer.setId(UUID.randomUUID());
        newCustomer.setEmail("new@example.com");
        newCustomer.setName("New Customer");

        when(customerService.getCustomerById(customerId))
                .thenThrow(new RuntimeException("Customer not found"));
        when(customerService.createCustomer(any(Customer.class))).thenReturn(newCustomer);
        when(subscriptionService.createSubscription(any(Subscription.class), anyList()))
                .thenReturn(subscription);

        ResponseEntity<Subscription> response = subscriptionController.createSubscription(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        verify(customerService).createCustomer(any(Customer.class));
    }

    @Test
    void getSubscription_Success() {
        when(subscriptionService.getSubscriptionById(subscriptionId)).thenReturn(subscription);

        ResponseEntity<Subscription> response = subscriptionController.getSubscription(subscriptionId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(subscriptionId);
    }

    @Test
    void getSubscriptionItems_Success() {
        List<SubscriptionItem> items = List.of(item);
        when(subscriptionService.getSubscriptionItems(subscriptionId)).thenReturn(items);

        ResponseEntity<List<SubscriptionItem>> response = subscriptionController.getSubscriptionItems(subscriptionId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getId()).isEqualTo(item.getId());
    }

    @Test
    void cancelSubscription_Success_Immediately() {
        Subscription canceledSubscription = new Subscription();
        canceledSubscription.setId(subscriptionId);
        canceledSubscription.setStatus(SubscriptionStatus.CANCELED);

        when(subscriptionService.cancelSubscription(subscriptionId, "Test reason", true))
                .thenReturn(canceledSubscription);

        ResponseEntity<Subscription> response = subscriptionController.cancelSubscription(subscriptionId, true, "Test reason");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
    }

    @Test
    void cancelSubscription_Success_AtPeriodEnd() {
        when(subscriptionService.cancelSubscription(subscriptionId, null, false))
                .thenReturn(subscription);

        ResponseEntity<Subscription> response = subscriptionController.cancelSubscription(subscriptionId, false, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void pauseSubscription_Success() {
        Subscription pausedSubscription = new Subscription();
        pausedSubscription.setId(subscriptionId);
        pausedSubscription.setStatus(SubscriptionStatus.PAUSED);

        when(subscriptionService.pauseSubscription(subscriptionId)).thenReturn(pausedSubscription);

        ResponseEntity<Subscription> response = subscriptionController.pauseSubscription(subscriptionId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(SubscriptionStatus.PAUSED);
    }

    @Test
    void resumeSubscription_Success() {
        Subscription resumedSubscription = new Subscription();
        resumedSubscription.setId(subscriptionId);
        resumedSubscription.setStatus(SubscriptionStatus.ACTIVE);

        when(subscriptionService.resumeSubscription(subscriptionId)).thenReturn(resumedSubscription);

        ResponseEntity<Subscription> response = subscriptionController.resumeSubscription(subscriptionId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }
}

