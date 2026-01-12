package com.fluxpay.billing.repository;

import com.fluxpay.billing.entity.Invoice;
import com.fluxpay.common.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT i FROM Invoice i WHERE i.tenantId = :tenantId AND i.deletedAt IS NULL AND (:status IS NULL OR i.status = :status)")
    Page<Invoice> findByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") InvoiceStatus status, Pageable pageable);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.tenantId = :tenantId AND i.deletedAt IS NULL")
    Long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.tenantId = :tenantId AND i.status = :status AND i.deletedAt IS NULL")
    Long countByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") InvoiceStatus status);

    @Query("SELECT COALESCE(SUM(i.total), 0) FROM Invoice i WHERE i.tenantId = :tenantId AND i.deletedAt IS NULL")
    Long sumTotalByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COALESCE(SUM(i.amountDue), 0) FROM Invoice i WHERE i.tenantId = :tenantId AND i.deletedAt IS NULL")
    Long sumAmountDueByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COALESCE(SUM(i.amountPaid), 0) FROM Invoice i WHERE i.tenantId = :tenantId AND i.deletedAt IS NULL")
    Long sumAmountPaidByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.tenantId = :tenantId AND i.status = 'OPEN' AND i.dueDate < :today AND i.deletedAt IS NULL")
    Long countOverdueByTenantId(@Param("tenantId") UUID tenantId, @Param("today") LocalDate today);

    @Query("SELECT COALESCE(SUM(i.amountDue), 0) FROM Invoice i WHERE i.tenantId = :tenantId AND i.status = 'OPEN' AND i.dueDate < :today AND i.deletedAt IS NULL")
    Long sumOverdueAmountByTenantId(@Param("tenantId") UUID tenantId, @Param("today") LocalDate today);
}

