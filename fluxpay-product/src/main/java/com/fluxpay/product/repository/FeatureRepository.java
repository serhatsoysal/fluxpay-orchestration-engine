package com.fluxpay.product.repository;

import com.fluxpay.product.entity.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeatureRepository extends JpaRepository<Feature, UUID> {

    List<Feature> findByTenantId(UUID tenantId);

    Optional<Feature> findByTenantIdAndFeatureKey(UUID tenantId, String featureKey);
}

