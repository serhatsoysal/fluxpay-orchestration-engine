package com.fluxpay.billing.repository;

import com.fluxpay.billing.entity.TaxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxRateRepository extends JpaRepository<TaxRate, UUID> {
    List<TaxRate> findByTenantIdAndActiveTrue(UUID tenantId);
    Optional<TaxRate> findByTenantIdAndCountryCodeAndActiveTrueOrderByCreatedAtDesc(UUID tenantId, String countryCode);
}

