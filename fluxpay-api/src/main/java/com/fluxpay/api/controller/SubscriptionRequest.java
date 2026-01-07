package com.fluxpay.api.controller;

import com.fluxpay.subscription.entity.Subscription;
import com.fluxpay.subscription.entity.SubscriptionItem;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SubscriptionRequest {
    private UUID customerId;
    private String customerEmail;
    private String customerName;
    private Subscription subscription;
    private List<SubscriptionItem> items;
}

