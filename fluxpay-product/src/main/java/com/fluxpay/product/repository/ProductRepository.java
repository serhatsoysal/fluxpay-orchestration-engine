package com.fluxpay.product.repository;

import com.fluxpay.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByTenantIdAndActive(UUID tenantId, Boolean active);

    List<Product> findByTenantId(UUID tenantId);
}

