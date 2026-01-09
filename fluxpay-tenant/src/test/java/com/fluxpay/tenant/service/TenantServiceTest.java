package com.fluxpay.tenant.service;

import com.fluxpay.common.enums.TenantStatus;
import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.common.exception.ValidationException;
import com.fluxpay.tenant.entity.Tenant;
import com.fluxpay.tenant.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantService tenantService;

    private Tenant tenant;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Test Tenant");
        tenant.setSlug("test-tenant");
        tenant.setBillingEmail("billing@test.com");
        tenant.setSupportEmail("support@test.com");
        tenant.setStatus(TenantStatus.ACTIVE);
    }

    @Test
    void createTenant_ShouldSucceed() {
        when(tenantRepository.existsBySlug(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        Tenant result = tenantService.createTenant(tenant);

        assertThat(result).isEqualTo(tenant);
        verify(tenantRepository).existsBySlug(tenant.getSlug());
        verify(tenantRepository).save(tenant);
    }

    @Test
    void createTenant_ShouldThrowException_WhenSlugExists() {
        when(tenantRepository.existsBySlug(anyString())).thenReturn(true);

        assertThatThrownBy(() -> tenantService.createTenant(tenant))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Tenant slug already exists");
        
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void getTenantById_ShouldReturnTenant() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        Tenant result = tenantService.getTenantById(tenantId);

        assertThat(result).isEqualTo(tenant);
        verify(tenantRepository).findById(tenantId);
    }

    @Test
    void getTenantById_ShouldThrowException_WhenNotFound() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.getTenantById(tenantId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getTenantBySlug_ShouldReturnTenant() {
        when(tenantRepository.findBySlug("test-tenant")).thenReturn(Optional.of(tenant));

        Tenant result = tenantService.getTenantBySlug("test-tenant");

        assertThat(result).isEqualTo(tenant);
    }

    @Test
    void updateTenant_ShouldSucceed() {
        Tenant updatedTenant = new Tenant();
        updatedTenant.setName("Updated Tenant");
        updatedTenant.setSlug("test-tenant");
        updatedTenant.setBillingEmail("new@test.com");
        updatedTenant.setSupportEmail("newsupport@test.com");

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        Tenant result = tenantService.updateTenant(tenantId, updatedTenant);

        assertThat(result.getName()).isEqualTo("Updated Tenant");
        verify(tenantRepository).save(tenant);
    }

    @Test
    void suspendTenant_ShouldSucceed() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        tenantService.suspendTenant(tenantId);

        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
        verify(tenantRepository).save(tenant);
    }

    @Test
    void activateTenant_ShouldSucceed() {
        tenant.setStatus(TenantStatus.SUSPENDED);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        tenantService.activateTenant(tenantId);

        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        verify(tenantRepository).save(tenant);
    }

    @Test
    void deleteTenant_ShouldSucceed() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        tenantService.deleteTenant(tenantId);

        assertThat(tenant.getDeletedAt()).isNotNull();
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.DELETED);
        verify(tenantRepository).save(tenant);
    }
}

