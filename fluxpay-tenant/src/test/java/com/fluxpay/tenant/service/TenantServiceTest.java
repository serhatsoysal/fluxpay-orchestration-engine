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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantService tenantService;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setName("Test Tenant");
        tenant.setSlug("test-tenant");
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setBillingEmail("billing@test.com");
    }

    @Test
    void createTenant_Success() {
        when(tenantRepository.existsBySlug(tenant.getSlug())).thenReturn(false);
        when(tenantRepository.save(tenant)).thenReturn(tenant);

        Tenant result = tenantService.createTenant(tenant);

        assertThat(result).isNotNull();
        assertThat(result.getSlug()).isEqualTo("test-tenant");
        verify(tenantRepository).save(tenant);
    }

    @Test
    void createTenant_ThrowsException_WhenSlugExists() {
        when(tenantRepository.existsBySlug(tenant.getSlug())).thenReturn(true);

        assertThatThrownBy(() -> tenantService.createTenant(tenant))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Tenant slug already exists");

        verify(tenantRepository, never()).save(any());
    }

    @Test
    void getTenantById_Success() {
        when(tenantRepository.findById(tenant.getId())).thenReturn(Optional.of(tenant));

        Tenant result = tenantService.getTenantById(tenant.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(tenant.getId());
    }

    @Test
    void getTenantById_ThrowsException_WhenNotFound() {
        UUID id = UUID.randomUUID();
        when(tenantRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.getTenantById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getTenantBySlug_Success() {
        when(tenantRepository.findBySlug(tenant.getSlug())).thenReturn(Optional.of(tenant));

        Tenant result = tenantService.getTenantBySlug(tenant.getSlug());

        assertThat(result).isNotNull();
        assertThat(result.getSlug()).isEqualTo("test-tenant");
    }

    @Test
    void getTenantBySlug_ThrowsException_WhenNotFound() {
        String slug = "nonexistent";
        when(tenantRepository.findBySlug(slug)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.getTenantBySlug(slug))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

