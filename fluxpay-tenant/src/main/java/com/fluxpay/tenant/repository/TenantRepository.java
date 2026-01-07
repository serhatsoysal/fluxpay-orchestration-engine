package com.fluxpay.tenant.repository;

import com.fluxpay.tenant.entity.Tenant;
import com.fluxpay.common.enums.TenantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findBySlug(String slug);

    Optional<Tenant> findByIdAndStatus(UUID id, TenantStatus status);

    boolean existsBySlug(String slug);
}

