package com.fluxpay.billing.repository;

import com.fluxpay.billing.entity.Payment;
import com.fluxpay.common.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId);

    List<Payment> findByInvoiceId(UUID invoiceId);

    List<Payment> findByStatus(PaymentStatus status);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}

