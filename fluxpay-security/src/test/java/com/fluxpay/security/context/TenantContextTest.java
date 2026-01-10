package com.fluxpay.security.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testSetAndGetCurrentTenant() {
        UUID tenantId = UUID.randomUUID();
        
        TenantContext.setCurrentTenant(tenantId);
        UUID retrievedTenantId = TenantContext.getCurrentTenantId();
        
        assertEquals(tenantId, retrievedTenantId);
    }

    @Test
    void testGetCurrentTenantWhenNotSet() {
        TenantContext.clear();
        UUID retrievedTenantId = TenantContext.getCurrentTenantId();
        
        assertNull(retrievedTenantId);
    }

    @Test
    void testClearTenantContext() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);
        
        TenantContext.clear();
        UUID retrievedTenantId = TenantContext.getCurrentTenantId();
        
        assertNull(retrievedTenantId);
    }

    @Test
    void testMultipleSetAndClear() {
        UUID tenantId1 = UUID.randomUUID();
        UUID tenantId2 = UUID.randomUUID();
        
        TenantContext.setCurrentTenant(tenantId1);
        assertEquals(tenantId1, TenantContext.getCurrentTenantId());
        
        TenantContext.setCurrentTenant(tenantId2);
        assertEquals(tenantId2, TenantContext.getCurrentTenantId());
        
        TenantContext.clear();
        assertNull(TenantContext.getCurrentTenantId());
    }
}

