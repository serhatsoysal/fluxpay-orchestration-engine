package com.fluxpay.billing.repository;

import com.fluxpay.billing.entity.Invoice;
import com.fluxpay.common.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId);

    List<Invoice> findBySubscriptionId(UUID subscriptionId);

    List<Invoice> findByStatusAndDueDateBefore(InvoiceStatus status, LocalDate dueDate);

    Optional<Invoice> findByIdempotencyKey(String idempotencyKey);

    Optional<Invoice> findTopByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}

