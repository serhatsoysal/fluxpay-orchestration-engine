package com.fluxpay.common.event;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID tenantId,
        UUID subscriptionId,
        UUID customerId,
        String status
) implements DomainEvent {
    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public UUID getTenantId() {
        return tenantId;
    }

    @Override
    public String getEventType() {
        return "subscription.created";
    }
}

