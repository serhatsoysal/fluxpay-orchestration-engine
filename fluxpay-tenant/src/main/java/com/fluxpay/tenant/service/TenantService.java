package com.fluxpay.tenant.service;

import com.fluxpay.common.enums.TenantStatus;
import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.common.exception.ValidationException;
import com.fluxpay.tenant.entity.Tenant;
import com.fluxpay.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public Tenant createTenant(Tenant tenant) {
        if (tenantRepository.existsBySlug(tenant.getSlug())) {
            throw new ValidationException("Tenant slug already exists: " + tenant.getSlug());
        }
        return tenantRepository.save(tenant);
    }

    @Transactional(readOnly = true)
    public Tenant getTenantById(UUID id) {
        return findTenantById(id);
    }

    private Tenant findTenantById(UUID id) {
        return tenantRepository.findById(id)
                .filter(t -> t.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", id));
    }

    @Transactional(readOnly = true)
    public Tenant getTenantBySlug(String slug) {
        return tenantRepository.findBySlug(slug)
                .filter(t -> t.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant with slug: " + slug));
    }

    public Tenant updateTenant(UUID id, Tenant updatedTenant) {
        Tenant tenant = findTenantById(id);
        
        if (!tenant.getSlug().equals(updatedTenant.getSlug()) 
                && tenantRepository.existsBySlug(updatedTenant.getSlug())) {
            throw new ValidationException("Tenant slug already exists: " + updatedTenant.getSlug());
        }

        tenant.setName(updatedTenant.getName());
        tenant.setSlug(updatedTenant.getSlug());
        tenant.setBillingEmail(updatedTenant.getBillingEmail());
        tenant.setSupportEmail(updatedTenant.getSupportEmail());
        tenant.setLogoUrl(updatedTenant.getLogoUrl());
        tenant.setPrimaryColor(updatedTenant.getPrimaryColor());

        return tenantRepository.save(tenant);
    }

    public void suspendTenant(UUID id) {
        Tenant tenant = findTenantById(id);
        tenant.setStatus(TenantStatus.SUSPENDED);
        tenantRepository.save(tenant);
    }

    public void activateTenant(UUID id) {
        Tenant tenant = findTenantById(id);
        tenant.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(tenant);
    }

    public void deleteTenant(UUID id) {
        Tenant tenant = findTenantById(id);
        tenant.softDelete();
        tenant.setStatus(TenantStatus.DELETED);
        tenantRepository.save(tenant);
    }
}

