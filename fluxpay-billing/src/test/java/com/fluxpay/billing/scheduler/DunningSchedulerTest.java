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
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
        invoice.setNextPaymentAttempt(null);

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

    @Test
    void retryFailedPayments_ShouldNotRetryWhenAttemptCountExceedsMax() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setAttemptCount(3);
        invoice.setDueDate(LocalDate.now().minusDays(1));

        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));

        scheduler.retryFailedPayments();

        verify(paymentService, never()).createPayment(any());
    }

    @Test
    void retryFailedPayments_ShouldNotRetryWhenDueDateNotPassed() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setAttemptCount(1);
        invoice.setDueDate(LocalDate.now().plusDays(1));

        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));

        scheduler.retryFailedPayments();

        verify(paymentService, never()).createPayment(any());
    }

    @Test
    void retryFailedPayments_ShouldNotRetryWhenStatusNotOpen() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setAttemptCount(1);
        invoice.setDueDate(LocalDate.now().minusDays(1));

        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));

        scheduler.retryFailedPayments();

        verify(paymentService, never()).createPayment(any());
    }

    @Test
    void retryFailedPayments_ShouldHandleFailedPayment() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setTenantId(UUID.randomUUID());
        invoice.setCustomerId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setAttemptCount(1);
        invoice.setDueDate(LocalDate.now().minusDays(1));
        invoice.setAmountDue(10000L);
        invoice.setCurrency("USD");
        invoice.setTotal(10000L);
        invoice.setNextPaymentAttempt(null);

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setStatus(PaymentStatus.FAILED);

        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));
        when(paymentService.createPayment(any())).thenReturn(payment);
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        scheduler.retryFailedPayments();

        verify(paymentService).createPayment(any(Payment.class));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void retryFailedPayments_ShouldHandleException() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setTenantId(UUID.randomUUID());
        invoice.setCustomerId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setAttemptCount(1);
        invoice.setDueDate(LocalDate.now().minusDays(1));
        invoice.setAmountDue(10000L);
        invoice.setCurrency("USD");
        invoice.setNextPaymentAttempt(null);

        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));
        when(paymentService.createPayment(any())).thenThrow(new RuntimeException("Payment failed"));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        scheduler.retryFailedPayments();

        verify(paymentService).createPayment(any(Payment.class));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void retryFailedPayments_ShouldMarkAsUncollectibleWhenMaxAttemptsReached() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setTenantId(UUID.randomUUID());
        invoice.setCustomerId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setAttemptCount(2);
        invoice.setDueDate(LocalDate.now().minusDays(1));
        invoice.setAmountDue(10000L);
        invoice.setCurrency("USD");
        invoice.setTotal(10000L);
        invoice.setNextPaymentAttempt(null);

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setStatus(PaymentStatus.FAILED);

        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));
        when(paymentService.createPayment(any())).thenReturn(payment);
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        scheduler.retryFailedPayments();

        verify(invoiceRepository).save(argThat(inv -> inv.getAttemptCount() == 3 && inv.getStatus() == InvoiceStatus.UNCOLLECTIBLE));
    }

    @Test
    void retryFailedPayments_ShouldSkipWhenNextAttemptNotDue() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setAttemptCount(1);
        invoice.setDueDate(LocalDate.now().minusDays(1));
        invoice.setNextPaymentAttempt(Instant.now().plus(1, ChronoUnit.DAYS));

        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));

        scheduler.retryFailedPayments();

        verify(paymentService, never()).createPayment(any());
    }

    @Test
    void retryFailedPayments_ShouldSkipDeletedInvoices() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setAttemptCount(1);
        invoice.setDueDate(LocalDate.now().minusDays(1));
        invoice.setDeletedAt(Instant.now());

        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));

        scheduler.retryFailedPayments();

        verify(paymentService, never()).createPayment(any());
    }

    @Test
    void retryFailedPayments_ShouldHandleEmptyList() {
        when(invoiceRepository.findAll()).thenReturn(Collections.emptyList());

        scheduler.retryFailedPayments();

        verify(paymentService, never()).createPayment(any());
    }

    @Test
    void retryFailedPayments_ShouldRetryWhenNextAttemptExactlyAtThreshold() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setTenantId(UUID.randomUUID());
        invoice.setCustomerId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setAttemptCount(1);
        invoice.setDueDate(LocalDate.now().minusDays(1));
        invoice.setAmountDue(10000L);
        invoice.setCurrency("USD");
        invoice.setTotal(10000L);
        invoice.setNextPaymentAttempt(Instant.now().minus(3, ChronoUnit.DAYS));

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

    @Test
    void retryFailedPayments_ShouldSkipWhenDueDateIsNull() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setAttemptCount(1);
        invoice.setDueDate(null);

        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));

        scheduler.retryFailedPayments();

        verify(paymentService, never()).createPayment(any());
    }

    @Test
    void retryFailedPayments_ShouldSetInvoiceCorrectlyOnSuccess() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setTenantId(UUID.randomUUID());
        invoice.setCustomerId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setAttemptCount(1);
        invoice.setDueDate(LocalDate.now().minusDays(1));
        invoice.setAmountDue(10000L);
        invoice.setCurrency("USD");
        invoice.setTotal(10000L);
        invoice.setNextPaymentAttempt(null);

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setStatus(PaymentStatus.SUCCEEDED);

        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));
        when(paymentService.createPayment(any())).thenReturn(payment);
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        scheduler.retryFailedPayments();

        verify(invoiceRepository).save(argThat(inv -> 
            inv.getStatus() == InvoiceStatus.PAID &&
            inv.getAmountPaid() == 10000L &&
            inv.getAmountDue() == 0L &&
            inv.getPaidAt() != null
        ));
    }

    @Test
    void retryFailedPayments_ShouldIncrementAttemptCountOnFailure() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setTenantId(UUID.randomUUID());
        invoice.setCustomerId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setAttemptCount(1);
        invoice.setDueDate(LocalDate.now().minusDays(1));
        invoice.setAmountDue(10000L);
        invoice.setCurrency("USD");
        invoice.setTotal(10000L);
        invoice.setNextPaymentAttempt(null);

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setStatus(PaymentStatus.FAILED);

        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));
        when(paymentService.createPayment(any())).thenReturn(payment);
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        scheduler.retryFailedPayments();

        verify(invoiceRepository).save(argThat(inv -> 
            inv.getAttemptCount() == 2 &&
            inv.getNextPaymentAttempt() != null
        ));
    }

    @Test
    void retryFailedPayments_ShouldSkipWhenDueDateEqualToToday() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setAttemptCount(1);
        invoice.setDueDate(LocalDate.now());

        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));

        scheduler.retryFailedPayments();

        verify(paymentService, never()).createPayment(any());
    }
}

