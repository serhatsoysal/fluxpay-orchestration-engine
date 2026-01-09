package com.fluxpay.common.exception;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionsTest {

    @Test
    void testValidationException_WithMessage() {
        ValidationException exception = new ValidationException("Validation failed");
        
        assertThat(exception.getMessage()).isEqualTo("Validation failed");
    }

    @Test
    void testValidationException_WithMessageAndCause() {
        Throwable cause = new RuntimeException("Root cause");
        ValidationException exception = new ValidationException("Validation failed", cause);
        
        assertThat(exception.getMessage()).isEqualTo("Validation failed");
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void testResourceNotFoundException_WithResourceAndId() {
        UUID id = UUID.randomUUID();
        ResourceNotFoundException exception = new ResourceNotFoundException("User", id);
        
        assertThat(exception.getMessage()).isEqualTo("User not found with id: " + id);
    }

    @Test
    void testResourceNotFoundException_WithMessage() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Custom message");
        
        assertThat(exception.getMessage()).isEqualTo("Custom message");
    }

    @Test
    void testTenantSuspendedException_WithTenantId() {
        UUID tenantId = UUID.randomUUID();
        TenantSuspendedException exception = new TenantSuspendedException(tenantId);
        
        assertThat(exception.getMessage()).isEqualTo("Tenant is suspended: " + tenantId);
    }

    @Test
    void testTenantSuspendedException_WithMessage() {
        TenantSuspendedException exception = new TenantSuspendedException("Custom message");
        
        assertThat(exception.getMessage()).isEqualTo("Custom message");
    }
}

