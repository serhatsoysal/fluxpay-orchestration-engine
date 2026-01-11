package com.fluxpay.common.event;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
    UUID getEventId();
    Instant getOccurredAt();
    UUID getTenantId();
    String getEventType();
}

