package com.fluxpay.product.repository;

import com.fluxpay.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByTenantIdAndActive(UUID tenantId, Boolean active);

    List<Product> findByTenantId(UUID tenantId);

    boolean existsByTenantIdAndName(UUID tenantId, String name);

    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE p.tenantId = :tenantId AND p.name = :name AND p.id != :excludeId AND p.deletedAt IS NULL")
    boolean existsByTenantIdAndNameExcludingId(@Param("tenantId") UUID tenantId, @Param("name") String name, @Param("excludeId") UUID excludeId);
}

