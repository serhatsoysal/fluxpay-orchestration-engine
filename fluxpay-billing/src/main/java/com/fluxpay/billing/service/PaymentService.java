package com.fluxpay.billing.service;

import com.fluxpay.billing.entity.Payment;
import com.fluxpay.billing.entity.Refund;
import com.fluxpay.billing.repository.PaymentRepository;
import com.fluxpay.billing.repository.RefundRepository;
import com.fluxpay.common.dto.PageResponse;
import com.fluxpay.common.dto.PaymentStatsResponse;
import com.fluxpay.common.dto.Period;
import com.fluxpay.common.enums.PaymentMethod;
import com.fluxpay.common.enums.PaymentStatus;
import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.common.exception.ValidationException;
import com.fluxpay.security.context.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;

    public PaymentService(PaymentRepository paymentRepository, RefundRepository refundRepository) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
    }

    public Payment createPayment(Payment payment) {
        if (payment == null) {
            throw new ValidationException("Payment cannot be null");
        }
        if (payment.getAmount() == null || payment.getAmount() <= 0) {
            throw new ValidationException("Payment amount must be greater than zero");
        }
        if (payment.getCustomerId() == null) {
            throw new ValidationException("Payment customer ID cannot be null");
        }
        
        payment.setStatus(PaymentStatus.PROCESSING);
        Payment savedPayment = paymentRepository.save(payment);
        
        boolean success = processPaymentWithProcessor();
        
        if (success) {
            savedPayment.setStatus(PaymentStatus.COMPLETED);
            savedPayment.setPaymentIntentId("pi_" + UUID.randomUUID().toString().replace("-", ""));
            savedPayment.setTransactionId("txn_" + UUID.randomUUID().toString().replace("-", ""));
            savedPayment.setPaidAt(Instant.now());
        } else {
            savedPayment.setStatus(PaymentStatus.FAILED);
            savedPayment.setFailureReason("Payment processor failed");
        }

        return paymentRepository.save(savedPayment);
    }

    @Transactional(readOnly = true)
    public Payment getPaymentById(UUID id) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return paymentRepository.findById(id)
                .filter(p -> p.getDeletedAt() == null && p.getTenantId() != null && p.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
    }

    @Transactional(readOnly = true)
    public PageResponse<Payment> getPayments(
            int page, int size,
            PaymentStatus status,
            PaymentMethod paymentMethod,
            UUID invoiceId,
            UUID customerId,
            LocalDate dateFrom,
            LocalDate dateTo,
            Long amountMin,
            Long amountMax) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        
        Instant dateFromInstant = dateFrom != null ? dateFrom.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant dateToInstant = dateTo != null ? dateTo.atTime(23, 59, 59).toInstant(ZoneOffset.UTC) : null;
        
        Page<Payment> paymentPage = paymentRepository.findPaymentsWithFilters(
                tenantId, status, paymentMethod, invoiceId, customerId,
                dateFromInstant, dateToInstant, amountMin, amountMax, pageable);
        
        return new PageResponse<>(
                paymentPage.getContent(),
                paymentPage.getNumber(),
                paymentPage.getSize(),
                paymentPage.getTotalElements(),
                paymentPage.getTotalPages(),
                paymentPage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByCustomer(UUID customerId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return paymentRepository.findByTenantIdAndCustomerId(tenantId, customerId);
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByInvoice(UUID invoiceId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return paymentRepository.findByInvoiceId(invoiceId).stream()
                .filter(p -> p.getTenantId() != null && p.getTenantId().equals(tenantId))
                .toList();
    }

    public Refund createRefund(UUID paymentId, Long amount, String reason, java.util.Map<String, Object> metadata) {
        if (amount == null || amount <= 0) {
            throw new ValidationException("Refund amount must be greater than zero");
        }
        
        Payment payment = getPaymentById(paymentId);
        
        if (payment.getStatus() != PaymentStatus.COMPLETED && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new ValidationException("Payment cannot be refunded. Current status: " + payment.getStatus());
        }
        
        Long currentRefundedAmount = payment.getRefundedAmount() != null ? payment.getRefundedAmount() : 0L;
        Long paymentAmount = payment.getAmount();
        if (paymentAmount == null) {
            throw new ValidationException("Payment amount cannot be null");
        }
        
        long refundableAmount = paymentAmount - currentRefundedAmount;
        if (amount > refundableAmount) {
            throw new ValidationException("Refund amount exceeds refundable amount");
        }
        
        Refund refund = new Refund();
        refund.setPaymentId(paymentId);
        refund.setAmount(amount);
        refund.setCurrency(payment.getCurrency());
        refund.setStatus(PaymentStatus.COMPLETED);
        refund.setReason(reason);
        refund.setRefundId("re_" + UUID.randomUUID().toString().replace("-", ""));
        refund.setMetadata(metadata);
        
        Refund savedRefund = refundRepository.save(refund);
        
        Long newRefundedAmount = currentRefundedAmount + amount;
        payment.setRefundedAmount(newRefundedAmount);
        if (newRefundedAmount.equals(paymentAmount)) {
            payment.setStatus(PaymentStatus.REFUNDED);
        } else {
            payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
        }
        paymentRepository.save(payment);
        
        return savedRefund;
    }

    @Transactional(readOnly = true)
    public PaymentStatsResponse getPaymentStats(LocalDate dateFrom, LocalDate dateTo) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        
        Instant dateFromInstant = dateFrom != null ? dateFrom.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant dateToInstant = dateTo != null ? dateTo.atTime(23, 59, 59).toInstant(ZoneOffset.UTC) : null;
        
        Long totalRevenue = paymentRepository.sumRevenueByTenantId(tenantId, dateFromInstant, dateToInstant);
        Long totalCount = paymentRepository.countByTenantId(tenantId, dateFromInstant, dateToInstant);
        Long completedCount = paymentRepository.countByTenantIdAndStatus(tenantId, PaymentStatus.COMPLETED, dateFromInstant, dateToInstant);
        Long failedCount = paymentRepository.countByTenantIdAndStatus(tenantId, PaymentStatus.FAILED, dateFromInstant, dateToInstant);
        Long pendingCount = paymentRepository.countByTenantIdAndStatus(tenantId, PaymentStatus.PENDING, dateFromInstant, dateToInstant);
        Long refundedAmount = paymentRepository.sumRefundedAmountByTenantId(tenantId, dateFromInstant, dateToInstant);
        Long avgPaymentAmount = paymentRepository.avgPaymentAmountByTenantId(tenantId, dateFromInstant, dateToInstant);
        
        return new PaymentStatsResponse(
                totalRevenue,
                totalCount,
                completedCount,
                failedCount,
                pendingCount,
                refundedAmount,
                avgPaymentAmount,
                "USD",
                new Period(dateFrom != null ? dateFrom : LocalDate.now().minusMonths(1),
                        dateTo != null ? dateTo : LocalDate.now())
        );
    }

    private boolean processPaymentWithProcessor() {
        return Math.random() > 0.1;
    }
}

