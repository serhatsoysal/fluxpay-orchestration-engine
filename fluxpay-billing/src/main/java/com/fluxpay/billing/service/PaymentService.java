package com.fluxpay.billing.service;

import com.fluxpay.billing.entity.Payment;
import com.fluxpay.billing.repository.PaymentRepository;
import com.fluxpay.common.enums.PaymentStatus;
import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.security.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Payment createPayment(Payment payment) {
        payment.setStatus(PaymentStatus.PROCESSING);
        Payment savedPayment = paymentRepository.save(payment);
        
        boolean success = processPaymentWithProcessor();
        
        if (success) {
            savedPayment.setStatus(PaymentStatus.SUCCEEDED);
            savedPayment.setProcessorPaymentId("mock_" + UUID.randomUUID());
        } else {
            savedPayment.setStatus(PaymentStatus.FAILED);
            savedPayment.setFailureCode("mock_failure");
            savedPayment.setFailureMessage("Mock payment processor failed");
        }

        return paymentRepository.save(savedPayment);
    }

    @Transactional(readOnly = true)
    public Payment getPaymentById(UUID id) {
        return paymentRepository.findById(id)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByCustomer(UUID customerId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return paymentRepository.findByTenantIdAndCustomerId(tenantId, customerId);
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByInvoice(UUID invoiceId) {
        return paymentRepository.findByInvoiceId(invoiceId);
    }

    private boolean processPaymentWithProcessor() {
        return Math.random() > 0.1;
    }
}

