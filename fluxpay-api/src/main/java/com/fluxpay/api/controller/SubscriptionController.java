package com.fluxpay.api.controller;

import com.fluxpay.subscription.entity.Customer;
import com.fluxpay.subscription.entity.Subscription;
import com.fluxpay.subscription.entity.SubscriptionItem;
import com.fluxpay.subscription.service.CustomerService;
import com.fluxpay.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final CustomerService customerService;

    public SubscriptionController(SubscriptionService subscriptionService, CustomerService customerService) {
        this.subscriptionService = subscriptionService;
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<Subscription> createSubscription(@Valid @RequestBody SubscriptionRequest request) {
        Customer customer;
        if (request.getCustomerId() != null) {
            try {
                customer = customerService.getCustomerById(request.getCustomerId());
            } catch (Exception e) {
                Customer newCustomer = new Customer();
                newCustomer.setEmail(request.getCustomerEmail());
                newCustomer.setName(request.getCustomerName());
                customer = customerService.createCustomer(newCustomer);
            }
        } else {
            Customer newCustomer = new Customer();
            newCustomer.setEmail(request.getCustomerEmail());
            newCustomer.setName(request.getCustomerName());
            customer = customerService.createCustomer(newCustomer);
        }

        Subscription subscription = request.getSubscription();
        subscription.setCustomerId(customer.getId());

        Subscription createdSubscription = subscriptionService.createSubscription(
                subscription,
                request.getItems()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(createdSubscription);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Subscription> getSubscription(@PathVariable UUID id) {
        Subscription subscription = subscriptionService.getSubscriptionById(id);
        return ResponseEntity.ok(subscription);
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<List<SubscriptionItem>> getSubscriptionItems(@PathVariable UUID id) {
        List<SubscriptionItem> items = subscriptionService.getSubscriptionItems(id);
        return ResponseEntity.ok(items);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Subscription> cancelSubscription(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "false") boolean immediately,
            @RequestParam(required = false) String reason) {
        Subscription subscription = subscriptionService.cancelSubscription(id, reason, immediately);
        return ResponseEntity.ok(subscription);
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<Subscription> pauseSubscription(@PathVariable UUID id) {
        Subscription subscription = subscriptionService.pauseSubscription(id);
        return ResponseEntity.ok(subscription);
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<Subscription> resumeSubscription(@PathVariable UUID id) {
        Subscription subscription = subscriptionService.resumeSubscription(id);
        return ResponseEntity.ok(subscription);
    }
}

