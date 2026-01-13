package com.fluxpay.billing.repository;

import com.fluxpay.billing.entity.Payment;
import com.fluxpay.common.enums.PaymentMethod;
import com.fluxpay.common.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID>, PaymentRepositoryCustom {

    List<Payment> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId);

    List<Payment> findByInvoiceId(UUID invoiceId);

    List<Payment> findByStatus(PaymentStatus status);

    @Query(value = "SELECT * FROM payments WHERE tenant_id = :tenantId AND metadata->>'idempotencyKey' = :idempotencyKey AND deleted_at IS NULL", nativeQuery = true)
    Optional<Payment> findByIdempotencyKey(@Param("tenantId") UUID tenantId, @Param("idempotencyKey") String idempotencyKey);


    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.tenantId = :tenantId " +
           "AND p.deletedAt IS NULL AND p.status = 'COMPLETED' " +
           "AND (:dateFrom IS NULL OR p.createdAt >= :dateFrom) " +
           "AND (:dateTo IS NULL OR p.createdAt <= :dateTo)")
    Long sumRevenueByTenantId(@Param("tenantId") UUID tenantId,
                               @Param("dateFrom") Instant dateFrom,
                               @Param("dateTo") Instant dateTo);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.tenantId = :tenantId AND p.deletedAt IS NULL " +
           "AND (:dateFrom IS NULL OR p.createdAt >= :dateFrom) " +
           "AND (:dateTo IS NULL OR p.createdAt <= :dateTo)")
    Long countByTenantId(@Param("tenantId") UUID tenantId,
                         @Param("dateFrom") Instant dateFrom,
                         @Param("dateTo") Instant dateTo);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.tenantId = :tenantId AND p.status = :status " +
           "AND p.deletedAt IS NULL " +
           "AND (:dateFrom IS NULL OR p.createdAt >= :dateFrom) " +
           "AND (:dateTo IS NULL OR p.createdAt <= :dateTo)")
    Long countByTenantIdAndStatus(@Param("tenantId") UUID tenantId,
                                   @Param("status") PaymentStatus status,
                                   @Param("dateFrom") Instant dateFrom,
                                   @Param("dateTo") Instant dateTo);

    @Query("SELECT COALESCE(SUM(p.refundedAmount), 0) FROM Payment p WHERE p.tenantId = :tenantId " +
           "AND p.deletedAt IS NULL " +
           "AND (:dateFrom IS NULL OR p.createdAt >= :dateFrom) " +
           "AND (:dateTo IS NULL OR p.createdAt <= :dateTo)")
    Long sumRefundedAmountByTenantId(@Param("tenantId") UUID tenantId,
                                     @Param("dateFrom") Instant dateFrom,
                                     @Param("dateTo") Instant dateTo);

    @Query("SELECT COALESCE(AVG(p.amount), 0) FROM Payment p WHERE p.tenantId = :tenantId " +
           "AND p.deletedAt IS NULL AND p.status = 'COMPLETED' " +
           "AND (:dateFrom IS NULL OR p.createdAt >= :dateFrom) " +
           "AND (:dateTo IS NULL OR p.createdAt <= :dateTo)")
    Long avgPaymentAmountByTenantId(@Param("tenantId") UUID tenantId,
                                    @Param("dateFrom") Instant dateFrom,
                                    @Param("dateTo") Instant dateTo);
}

