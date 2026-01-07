package com.fluxpay.common.exception;

import java.util.UUID;

public class TenantSuspendedException extends RuntimeException {

    public TenantSuspendedException(UUID tenantId) {
        super("Tenant is suspended: " + tenantId);
    }

    public TenantSuspendedException(String message) {
        super(message);
    }
}

