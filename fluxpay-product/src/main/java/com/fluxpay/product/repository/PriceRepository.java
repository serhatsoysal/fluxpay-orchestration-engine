package com.fluxpay.product.repository;

import com.fluxpay.product.entity.Price;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PriceRepository extends JpaRepository<Price, UUID> {

    List<Price> findByProductIdAndActive(UUID productId, Boolean active);

    List<Price> findByProductId(UUID productId);
}

