package com.fluxpay.billing.repository;

import com.fluxpay.billing.entity.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, UUID> {
    List<Discount> findBySubscriptionId(UUID subscriptionId);
    List<Discount> findByInvoiceId(UUID invoiceId);
}

