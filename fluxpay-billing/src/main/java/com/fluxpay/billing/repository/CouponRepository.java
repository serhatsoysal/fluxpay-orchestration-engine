package com.fluxpay.billing.repository;

import com.fluxpay.billing.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {
    Optional<Coupon> findByTenantIdAndCode(UUID tenantId, String code);
}

