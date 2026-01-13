package com.fluxpay.billing.scheduler;

import com.fluxpay.billing.entity.Invoice;
import com.fluxpay.billing.entity.Payment;
import com.fluxpay.billing.repository.InvoiceRepository;
import com.fluxpay.billing.service.PaymentService;
import com.fluxpay.common.enums.InvoiceStatus;
import com.fluxpay.common.enums.PaymentStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class DunningScheduler {

    private final InvoiceRepository invoiceRepository;
    private final PaymentService paymentService;

    @Value("${DUNNING_RETRY_INTERVAL_DAYS:3}")
    private int retryIntervalDays;

    @Value("${DUNNING_MAX_ATTEMPTS:3}")
    private int maxAttempts;

    @Value("${DUNNING_OVERDUE_THRESHOLD_DAYS:3}")
    private int overdueThresholdDays;


    public DunningScheduler(InvoiceRepository invoiceRepository, PaymentService paymentService) {
        this.invoiceRepository = invoiceRepository;
        this.paymentService = paymentService;
    }

    @Scheduled(cron = "${DUNNING_RETRY_CRON:0 0 5 * * ?}")
    @Transactional
    public void retryFailedPayments() {
        List<Invoice> failedInvoices = invoiceRepository.findAll().stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.OPEN)
                .filter(inv -> inv.getAttemptCount() < maxAttempts)
                .filter(inv -> inv.getDueDate() != null && inv.getDueDate().isBefore(LocalDate.now()))
                .filter(inv -> inv.getDeletedAt() == null)
                .toList();

        for (Invoice invoice : failedInvoices) {
            Instant lastAttempt = invoice.getNextPaymentAttempt();
            if (lastAttempt == null || lastAttempt.isBefore(Instant.now().minus(retryIntervalDays, ChronoUnit.DAYS))) {
                retryPayment(invoice);
            }
        }
    }

    private void retryPayment(Invoice invoice) {
        Payment payment = new Payment();
        payment.setTenantId(invoice.getTenantId());
        payment.setCustomerId(invoice.getCustomerId());
        payment.setInvoiceId(invoice.getId());
        payment.setAmount(invoice.getAmountDue());
        payment.setCurrency(invoice.getCurrency());
        payment.setStatus(PaymentStatus.PENDING);

        try {
            Payment processedPayment = paymentService.createPayment(payment);
            
            if (processedPayment.getStatus() == PaymentStatus.COMPLETED) {
                invoice.setStatus(InvoiceStatus.PAID);
                invoice.setPaidAt(Instant.now());
                invoice.setAmountPaid(invoice.getTotal());
                invoice.setAmountDue(0L);
            } else {
                invoice.setAttemptCount(invoice.getAttemptCount() + 1);
                invoice.setNextPaymentAttempt(Instant.now().plus(retryIntervalDays, ChronoUnit.DAYS));
                
                if (invoice.getAttemptCount() >= maxAttempts) {
                    invoice.setStatus(InvoiceStatus.UNCOLLECTIBLE);
                }
            }
        } catch (Exception e) {
            invoice.setAttemptCount(invoice.getAttemptCount() + 1);
            invoice.setNextPaymentAttempt(Instant.now().plus(retryIntervalDays, ChronoUnit.DAYS));
        }

        invoiceRepository.save(invoice);
    }
}

