package com.fluxpay.billing.scheduler;

import com.fluxpay.billing.entity.Invoice;
import com.fluxpay.billing.entity.Payment;
import com.fluxpay.billing.repository.InvoiceRepository;
import com.fluxpay.billing.service.PaymentService;
import com.fluxpay.common.enums.InvoiceStatus;
import com.fluxpay.common.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DunningSchedulerTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PaymentService paymentService;

    private DunningScheduler scheduler;

    @BeforeEach
    void setUp() throws Exception {
        scheduler = new DunningScheduler(invoiceRepository, paymentService);
        
        setField(scheduler, "retryIntervalDays", 3);
        setField(scheduler, "maxAttempts", 3);
        setField(scheduler, "overdueThresholdDays", 3);
        setField(scheduler, "paymentProcessorName", "mock");
    }

    private void setField(Object target, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = DunningScheduler.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void retryFailedPayments_ShouldRetryEligibleInvoices() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setTenantId(UUID.randomUUID());
        invoice.setCustomerId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setAttemptCount(1);
        invoice.setDueDate(LocalDate.now().minusDays(1));
        invoice.setAmountDue(10000L);
        invoice.setCurrency("USD");

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setStatus(PaymentStatus.SUCCEEDED);

        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));
        when(paymentService.createPayment(any())).thenReturn(payment);
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        scheduler.retryFailedPayments();

        verify(paymentService).createPayment(any(Payment.class));
        verify(invoiceRepository).save(any(Invoice.class));
    }
}

