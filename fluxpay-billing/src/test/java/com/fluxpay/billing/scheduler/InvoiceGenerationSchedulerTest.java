package com.fluxpay.billing.scheduler;

import com.fluxpay.billing.entity.Invoice;
import com.fluxpay.billing.repository.InvoiceRepository;
import com.fluxpay.billing.service.InvoiceService;
import com.fluxpay.common.enums.InvoiceStatus;
import com.fluxpay.common.enums.SubscriptionStatus;
import com.fluxpay.subscription.entity.Subscription;
import com.fluxpay.subscription.repository.SubscriptionItemRepository;
import com.fluxpay.subscription.repository.SubscriptionRepository;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceGenerationSchedulerTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionItemRepository subscriptionItemRepository;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private InvoiceRepository invoiceRepository;

    private InvoiceGenerationScheduler scheduler;

    @BeforeEach
    void setUp() throws Exception {
        scheduler = new InvoiceGenerationScheduler(
                subscriptionRepository,
                subscriptionItemRepository,
                invoiceService,
                invoiceRepository);
        
        setField(scheduler, "invoiceGenerationDaysAhead", 3);
        setField(scheduler, "invoiceDueDays", 14);
        setField(scheduler, "invoicePeriodDays", 30);
        setField(scheduler, "invoiceDefaultCurrency", "USD");
        setField(scheduler, "invoiceItemUnitAmount", 1000L);
    }

    private void setField(Object target, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = InvoiceGenerationScheduler.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void generateUpcomingRenewalInvoices_ShouldCreateInvoicesForRenewals() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setTenantId(UUID.randomUUID());
        subscription.setCustomerId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodEnd(Instant.now().plus(2, ChronoUnit.DAYS));

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));
        when(invoiceRepository.findBySubscriptionId(any())).thenReturn(Collections.emptyList());
        when(subscriptionItemRepository.findBySubscriptionId(any())).thenReturn(Collections.emptyList());

        scheduler.generateUpcomingRenewalInvoices();

        verify(invoiceService).createInvoice(any(), any());
    }

    @Test
    void detectOverdueInvoices_ShouldMarkOverdueInvoices() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setDueDate(LocalDate.now().minusDays(1));

        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        scheduler.detectOverdueInvoices();

        verify(invoiceRepository).save(argThat(inv -> inv.getStatus() == InvoiceStatus.UNCOLLECTIBLE));
    }

    @Test
    void generateUpcomingRenewalInvoices_ShouldSkipWhenInvoiceExists() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setTenantId(UUID.randomUUID());
        subscription.setCustomerId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodEnd(Instant.now().plus(2, ChronoUnit.DAYS));

        Invoice existingInvoice = new Invoice();
        existingInvoice.setPeriodStart(subscription.getCurrentPeriodEnd());

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));
        when(invoiceRepository.findBySubscriptionId(any())).thenReturn(List.of(existingInvoice));

        scheduler.generateUpcomingRenewalInvoices();

        verify(invoiceService, never()).createInvoice(any(), any());
    }

    @Test
    void generateUpcomingRenewalInvoices_ShouldSkipNonActiveSubscriptions() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscription.setCurrentPeriodEnd(Instant.now().plus(2, ChronoUnit.DAYS));

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));

        scheduler.generateUpcomingRenewalInvoices();

        verify(invoiceService, never()).createInvoice(any(), any());
    }

    @Test
    void detectOverdueInvoices_ShouldSkipNonOpenInvoices() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setDueDate(LocalDate.now().minusDays(1));

        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));

        scheduler.detectOverdueInvoices();

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void detectOverdueInvoices_ShouldSkipDeletedInvoices() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setDueDate(LocalDate.now().minusDays(1));
        invoice.setDeletedAt(Instant.now());

        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));

        scheduler.detectOverdueInvoices();

        verify(invoiceRepository, never()).save(any());
    }
}

