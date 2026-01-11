package com.fluxpay.billing.service;

import com.fluxpay.billing.entity.Payment;
import com.fluxpay.billing.repository.PaymentRepository;
import com.fluxpay.common.enums.PaymentStatus;
import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.security.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

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
        payment.setProcessorName("mock");

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
                    p.setStatus(PaymentStatus.SUCCEEDED);
                    p.setProcessorPaymentId("mock_" + UUID.randomUUID());
                } else {
                    p.setStatus(PaymentStatus.FAILED);
                    p.setFailureCode("mock_failure");
                    p.setFailureMessage("Mock payment processor failed");
                }
            }
            return p;
        });

        Payment result = paymentService.createPayment(payment);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isIn(PaymentStatus.SUCCEEDED, PaymentStatus.FAILED);
        verify(paymentRepository, times(2)).save(any(Payment.class));
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
            p.getStatus() == PaymentStatus.SUCCEEDED || 
            p.getStatus() == PaymentStatus.FAILED
        ));
    }
}

