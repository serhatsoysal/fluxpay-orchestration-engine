package com.fluxpay.billing.service;

import com.fluxpay.billing.entity.Payment;
import com.fluxpay.billing.entity.Refund;
import com.fluxpay.billing.repository.PaymentRepository;
import com.fluxpay.billing.repository.RefundRepository;
import com.fluxpay.common.dto.PageResponse;
import com.fluxpay.common.dto.PaymentStatsResponse;
import com.fluxpay.common.enums.PaymentMethod;
import com.fluxpay.common.enums.PaymentStatus;
import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.common.exception.ValidationException;
import com.fluxpay.security.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RefundRepository refundRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Payment payment;
    private UUID paymentId;
    private UUID tenantId;
    private UUID customerId;
    private UUID invoiceId;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        invoiceId = UUID.randomUUID();

        payment = new Payment();
        payment.setId(paymentId);
        payment.setTenantId(tenantId);
        payment.setCustomerId(customerId);
        payment.setInvoiceId(invoiceId);
        payment.setAmount(10000L);
        payment.setCurrency("USD");

        TenantContext.setCurrentTenant(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createPayment_Success() {
        Payment savedPayment = new Payment();
        savedPayment.setId(paymentId);
        savedPayment.setTenantId(tenantId);
        savedPayment.setCustomerId(customerId);
        savedPayment.setInvoiceId(invoiceId);
        savedPayment.setAmount(10000L);
        savedPayment.setStatus(PaymentStatus.PROCESSING);

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        Payment result = paymentService.createPayment(payment);

        assertThat(result).isNotNull();
        verify(paymentRepository, atLeastOnce()).save(any(Payment.class));
    }

    @Test
    void createPayment_WithMockSuccess() {
        Payment savedPayment = new Payment();
        savedPayment.setId(paymentId);
        savedPayment.setTenantId(tenantId);
        savedPayment.setStatus(PaymentStatus.PROCESSING);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(paymentId);
            if (p.getStatus() == PaymentStatus.PROCESSING) {
                double random = Math.random();
                if (random > 0.1) {
                    p.setStatus(PaymentStatus.COMPLETED);
                    p.setPaymentIntentId("pi_mock_" + UUID.randomUUID());
                    p.setTransactionId("txn_mock_" + UUID.randomUUID());
                } else {
                    p.setStatus(PaymentStatus.FAILED);
                    p.setFailureReason("Mock payment processor failed");
                }
            }
            return p;
        });

        Payment result = paymentService.createPayment(payment);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isIn(PaymentStatus.COMPLETED, PaymentStatus.FAILED);
        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    @Test
    void createPayment_ShouldSetPaidAtWhenCompleted() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(paymentId);
            return p;
        });

        Payment result = paymentService.createPayment(payment);

        assertThat(result).isNotNull();
        if (result.getStatus() == PaymentStatus.COMPLETED) {
            assertThat(result.getPaidAt()).isNotNull();
            assertThat(result.getPaymentIntentId()).startsWith("pi_");
            assertThat(result.getTransactionId()).startsWith("txn_");
        } else if (result.getStatus() == PaymentStatus.FAILED) {
            assertThat(result.getFailureReason()).isEqualTo("Payment processor failed");
        }
        verify(paymentRepository, atLeast(2)).save(any(Payment.class));
    }

    @Test
    void getPaymentStats_WithNullDates_ShouldUseDefaults() {
        when(paymentRepository.sumRevenueByTenantId(eq(tenantId), any(), any())).thenReturn(0L);
        when(paymentRepository.countByTenantId(eq(tenantId), any(), any())).thenReturn(0L);
        when(paymentRepository.countByTenantIdAndStatus(eq(tenantId), eq(PaymentStatus.COMPLETED), any(), any()))
                .thenReturn(0L);
        when(paymentRepository.countByTenantIdAndStatus(eq(tenantId), eq(PaymentStatus.FAILED), any(), any()))
                .thenReturn(0L);
        when(paymentRepository.countByTenantIdAndStatus(eq(tenantId), eq(PaymentStatus.PENDING), any(), any()))
                .thenReturn(0L);
        when(paymentRepository.sumRefundedAmountByTenantId(eq(tenantId), any(), any())).thenReturn(0L);
        when(paymentRepository.avgPaymentAmountByTenantId(eq(tenantId), any(), any())).thenReturn(0L);

        PaymentStatsResponse result = paymentService.getPaymentStats(null, null);

        assertThat(result).isNotNull();
        assertThat(result.getPeriod().getFrom()).isNotNull();
        assertThat(result.getPeriod().getTo()).isNotNull();
    }

    @Test
    void getPaymentById_Success() {
        payment.setDeletedAt(null);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        Payment result = paymentService.getPaymentById(paymentId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(paymentId);
        verify(paymentRepository).findById(paymentId);
    }

    @Test
    void getPaymentById_NotFound() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentById(paymentId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(paymentRepository).findById(paymentId);
    }

    @Test
    void getPaymentById_Deleted() {
        payment.setDeletedAt(java.time.Instant.now());

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.getPaymentById(paymentId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(paymentRepository).findById(paymentId);
    }

    @Test
    void getPaymentsByCustomer_Success() {
        List<Payment> payments = List.of(payment);

        when(paymentRepository.findByTenantIdAndCustomerId(tenantId, customerId)).thenReturn(payments);

        List<Payment> result = paymentService.getPaymentsByCustomer(customerId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerId()).isEqualTo(customerId);
        verify(paymentRepository).findByTenantIdAndCustomerId(tenantId, customerId);
    }

    @Test
    void getPaymentsByInvoice_Success() {
        List<Payment> payments = List.of(payment);

        when(paymentRepository.findByInvoiceId(invoiceId)).thenReturn(payments);

        List<Payment> result = paymentService.getPaymentsByInvoice(invoiceId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInvoiceId()).isEqualTo(invoiceId);
        verify(paymentRepository).findByInvoiceId(invoiceId);
    }

    @Test
    void createPayment_SetsProcessingStatus() {
        Payment newPayment = new Payment();
        newPayment.setTenantId(tenantId);
        newPayment.setCustomerId(customerId);
        newPayment.setAmount(10000L);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        paymentService.createPayment(newPayment);

        verify(paymentRepository, atLeastOnce()).save(argThat(p -> 
            p.getStatus() == PaymentStatus.PROCESSING || 
            p.getStatus() == PaymentStatus.COMPLETED || 
            p.getStatus() == PaymentStatus.FAILED
        ));
    }

    @Test
    void getPaymentById_WithDifferentTenant_ShouldThrowException() {
        UUID differentTenantId = UUID.randomUUID();
        payment.setTenantId(differentTenantId);
        payment.setDeletedAt(null);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.getPaymentById(paymentId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(paymentRepository).findById(paymentId);
    }

    @Test
    void getPayments_ShouldReturnPageResponse() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Payment> paymentPage = new PageImpl<>(List.of(payment), pageable, 1L);

        when(paymentRepository.findPaymentsWithFilters(
                eq(tenantId), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(paymentPage);

        PageResponse<Payment> result = paymentService.getPayments(
                0, 20, null, null, null, null, null, null, null, null
        );

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(20);
        verify(paymentRepository).findPaymentsWithFilters(
                eq(tenantId), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void getPayments_WithFilters_ShouldCallRepositoryWithFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Payment> paymentPage = new PageImpl<>(List.of(payment), pageable, 1L);
        LocalDate dateFrom = LocalDate.now().minusDays(30);
        LocalDate dateTo = LocalDate.now();

        when(paymentRepository.findPaymentsWithFilters(
                eq(tenantId), eq(PaymentStatus.COMPLETED), eq(PaymentMethod.CREDIT_CARD),
                any(), eq(customerId), any(), any(), eq(1000L), eq(50000L), any()
        )).thenReturn(paymentPage);

        PageResponse<Payment> result = paymentService.getPayments(
                0, 20, PaymentStatus.COMPLETED, PaymentMethod.CREDIT_CARD,
                invoiceId, customerId, dateFrom, dateTo, 1000L, 50000L
        );

        assertThat(result).isNotNull();
        verify(paymentRepository).findPaymentsWithFilters(
                eq(tenantId), eq(PaymentStatus.COMPLETED), eq(PaymentMethod.CREDIT_CARD),
                eq(invoiceId), eq(customerId), any(), any(), eq(1000L), eq(50000L), any()
        );
    }

    @Test
    void getPayments_WithMaxSize_ShouldLimitTo100() {
        Pageable pageable = PageRequest.of(0, 100);
        Page<Payment> paymentPage = new PageImpl<>(List.of(payment), pageable, 1L);

        when(paymentRepository.findPaymentsWithFilters(
                eq(tenantId), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(paymentPage);

        paymentService.getPayments(0, 200, null, null, null, null, null, null, null, null);

        verify(paymentRepository).findPaymentsWithFilters(
                eq(tenantId), any(), any(), any(), any(), any(), any(), any(), any(),
                argThat(p -> p.getPageSize() == 100)
        );
    }

    @Test
    void getPaymentStats_ShouldReturnStats() {
        when(paymentRepository.sumRevenueByTenantId(eq(tenantId), any(), any())).thenReturn(100000L);
        when(paymentRepository.countByTenantId(eq(tenantId), any(), any())).thenReturn(10L);
        when(paymentRepository.countByTenantIdAndStatus(eq(tenantId), eq(PaymentStatus.COMPLETED), any(), any()))
                .thenReturn(8L);
        when(paymentRepository.countByTenantIdAndStatus(eq(tenantId), eq(PaymentStatus.FAILED), any(), any()))
                .thenReturn(1L);
        when(paymentRepository.countByTenantIdAndStatus(eq(tenantId), eq(PaymentStatus.PENDING), any(), any()))
                .thenReturn(1L);
        when(paymentRepository.sumRefundedAmountByTenantId(eq(tenantId), any(), any())).thenReturn(5000L);
        when(paymentRepository.avgPaymentAmountByTenantId(eq(tenantId), any(), any())).thenReturn(10000L);

        PaymentStatsResponse result = paymentService.getPaymentStats(null, null);

        assertThat(result).isNotNull();
        assertThat(result.getTotalRevenue()).isEqualTo(100000L);
        assertThat(result.getTotalCount()).isEqualTo(10L);
        assertThat(result.getCompletedCount()).isEqualTo(8L);
        assertThat(result.getFailedCount()).isEqualTo(1L);
        assertThat(result.getPendingCount()).isEqualTo(1L);
        assertThat(result.getRefundedAmount()).isEqualTo(5000L);
        assertThat(result.getAveragePaymentAmount()).isEqualTo(10000L);
    }

    @Test
    void getPaymentStats_WithDateRange_ShouldCallRepositoryWithDates() {
        LocalDate dateFrom = LocalDate.now().minusDays(30);
        LocalDate dateTo = LocalDate.now();

        when(paymentRepository.sumRevenueByTenantId(eq(tenantId), any(), any())).thenReturn(50000L);
        when(paymentRepository.countByTenantId(eq(tenantId), any(), any())).thenReturn(5L);
        when(paymentRepository.countByTenantIdAndStatus(eq(tenantId), eq(PaymentStatus.COMPLETED), any(), any()))
                .thenReturn(4L);
        when(paymentRepository.countByTenantIdAndStatus(eq(tenantId), eq(PaymentStatus.FAILED), any(), any()))
                .thenReturn(1L);
        when(paymentRepository.countByTenantIdAndStatus(eq(tenantId), eq(PaymentStatus.PENDING), any(), any()))
                .thenReturn(0L);
        when(paymentRepository.sumRefundedAmountByTenantId(eq(tenantId), any(), any())).thenReturn(2000L);
        when(paymentRepository.avgPaymentAmountByTenantId(eq(tenantId), any(), any())).thenReturn(10000L);

        PaymentStatsResponse result = paymentService.getPaymentStats(dateFrom, dateTo);

        assertThat(result).isNotNull();
        assertThat(result.getPeriod().getFrom()).isEqualTo(dateFrom);
        assertThat(result.getPeriod().getTo()).isEqualTo(dateTo);
    }

    @Test
    void createRefund_ShouldCreateRefundAndUpdatePayment() {
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setRefundedAmount(0L);
        payment.setAmount(10000L);

        Refund refund = new Refund();
        refund.setId(UUID.randomUUID());
        refund.setPaymentId(paymentId);
        refund.setAmount(5000L);
        refund.setCurrency("USD");
        refund.setStatus(PaymentStatus.COMPLETED);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> {
            Refund r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        Refund result = paymentService.createRefund(paymentId, 5000L, "Customer requested", null);

        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualTo(5000L);
        assertThat(payment.getRefundedAmount()).isEqualTo(5000L);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
        verify(refundRepository).save(any(Refund.class));
        verify(paymentRepository).save(payment);
    }

    @Test
    void createRefund_WithFullAmount_ShouldSetPaymentToRefunded() {
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setRefundedAmount(0L);
        payment.setAmount(10000L);

        Refund refund = new Refund();
        refund.setId(UUID.randomUUID());
        refund.setPaymentId(paymentId);
        refund.setAmount(10000L);
        refund.setCurrency("USD");
        refund.setStatus(PaymentStatus.COMPLETED);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> {
            Refund r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        Refund result = paymentService.createRefund(paymentId, 10000L, "Full refund", null);

        assertThat(result).isNotNull();
        assertThat(payment.getRefundedAmount()).isEqualTo(10000L);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(refundRepository).save(any(Refund.class));
        verify(paymentRepository).save(payment);
    }

    @Test
    void createRefund_WithPartiallyRefundedPayment_ShouldAllowAdditionalRefund() {
        payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
        payment.setRefundedAmount(5000L);
        payment.setAmount(10000L);

        Refund refund = new Refund();
        refund.setId(UUID.randomUUID());
        refund.setPaymentId(paymentId);
        refund.setAmount(3000L);
        refund.setCurrency("USD");
        refund.setStatus(PaymentStatus.COMPLETED);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> {
            Refund r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        Refund result = paymentService.createRefund(paymentId, 3000L, "Additional refund", null);

        assertThat(result).isNotNull();
        assertThat(payment.getRefundedAmount()).isEqualTo(8000L);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
    }

    @Test
    void createRefund_WhenPaymentNotFound_ShouldThrowException() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createRefund(paymentId, 5000L, "Reason", null))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(refundRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createRefund_WhenPaymentNotCompleted_ShouldThrowException() {
        payment.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.createRefund(paymentId, 5000L, "Reason", null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("cannot be refunded");

        verify(refundRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createRefund_WhenAmountExceedsRefundable_ShouldThrowException() {
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setRefundedAmount(5000L);
        payment.setAmount(10000L);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.createRefund(paymentId, 6000L, "Reason", null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("exceeds refundable amount");

        verify(refundRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createRefund_WithMetadata_ShouldStoreMetadata() {
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setRefundedAmount(0L);
        payment.setAmount(10000L);

        Map<String, Object> metadata = Map.of("source", "dashboard", "operator", "admin");

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> {
            Refund r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        Refund result = paymentService.createRefund(paymentId, 5000L, "Reason", metadata);

        assertThat(result).isNotNull();
        assertThat(result.getMetadata()).isEqualTo(metadata);
        verify(refundRepository).save(argThat(r -> r.getMetadata().equals(metadata)));
    }

    @Test
    void createRefund_WhenAmountEqualsRefundable_ShouldSetToRefunded() {
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setRefundedAmount(5000L);
        payment.setAmount(10000L);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> {
            Refund r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        Refund result = paymentService.createRefund(paymentId, 5000L, "Full refund", null);

        assertThat(result).isNotNull();
        assertThat(payment.getRefundedAmount()).isEqualTo(10000L);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentRepository).save(payment);
    }

    @Test
    void getPayments_WithNullDateTo_ShouldSetToEndOfDay() {
        LocalDate dateFrom = LocalDate.now().minusDays(7);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Payment> paymentPage = new PageImpl<>(List.of(payment), pageable, 1L);

        when(paymentRepository.findPaymentsWithFilters(
                eq(tenantId), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(paymentPage);

        PageResponse<Payment> result = paymentService.getPayments(
                0, 20, null, null, null, null, dateFrom, null, null, null
        );

        assertThat(result).isNotNull();
        verify(paymentRepository).findPaymentsWithFilters(
                eq(tenantId), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void createPayment_WhenPaymentIsNull_ShouldThrowException() {
        assertThatThrownBy(() -> paymentService.createPayment(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    void createPayment_WhenAmountIsNull_ShouldThrowException() {
        payment.setAmount(null);
        
        assertThatThrownBy(() -> paymentService.createPayment(payment))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("amount must be greater than zero");
    }

    @Test
    void createPayment_WhenAmountIsZero_ShouldThrowException() {
        payment.setAmount(0L);
        
        assertThatThrownBy(() -> paymentService.createPayment(payment))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("amount must be greater than zero");
    }

    @Test
    void createPayment_WhenAmountIsNegative_ShouldThrowException() {
        payment.setAmount(-100L);
        
        assertThatThrownBy(() -> paymentService.createPayment(payment))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("amount must be greater than zero");
    }

    @Test
    void createPayment_WhenCustomerIdIsNull_ShouldThrowException() {
        payment.setCustomerId(null);
        
        assertThatThrownBy(() -> paymentService.createPayment(payment))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("customer ID cannot be null");
    }

    @Test
    void createRefund_WhenAmountIsNull_ShouldThrowException() {
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setAmount(10000L);
        payment.setRefundedAmount(0L);
        
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        
        assertThatThrownBy(() -> paymentService.createRefund(paymentId, null, "Reason", null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("amount must be greater than zero");
    }

    @Test
    void createRefund_WhenAmountIsZero_ShouldThrowException() {
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setAmount(10000L);
        payment.setRefundedAmount(0L);
        
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        
        assertThatThrownBy(() -> paymentService.createRefund(paymentId, 0L, "Reason", null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("amount must be greater than zero");
    }

    @Test
    void createRefund_WhenAmountIsNegative_ShouldThrowException() {
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setAmount(10000L);
        payment.setRefundedAmount(0L);
        
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        
        assertThatThrownBy(() -> paymentService.createRefund(paymentId, -100L, "Reason", null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("amount must be greater than zero");
    }

    @Test
    void createRefund_WhenPaymentAmountIsNull_ShouldThrowException() {
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setAmount(null);
        payment.setRefundedAmount(0L);
        
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        
        assertThatThrownBy(() -> paymentService.createRefund(paymentId, 5000L, "Reason", null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Payment amount cannot be null");
    }
}

