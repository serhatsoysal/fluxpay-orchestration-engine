package com.fluxpay.billing.scheduler;

import com.fluxpay.billing.entity.Invoice;
import com.fluxpay.billing.repository.InvoiceRepository;
import com.fluxpay.billing.service.InvoiceService;
import com.fluxpay.common.enums.InvoiceStatus;
import com.fluxpay.common.enums.SubscriptionStatus;
import com.fluxpay.subscription.entity.Subscription;
import com.fluxpay.subscription.entity.SubscriptionItem;
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
import static org.mockito.ArgumentMatchers.argThat;
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

    @Test
    void generateUpcomingRenewalInvoices_ShouldGenerateInvoiceWithMultipleItems() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setTenantId(UUID.randomUUID());
        subscription.setCustomerId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodEnd(Instant.now().plus(2, ChronoUnit.DAYS));

        SubscriptionItem item1 = new SubscriptionItem();
        item1.setId(UUID.randomUUID());
        item1.setQuantity(5);

        SubscriptionItem item2 = new SubscriptionItem();
        item2.setId(UUID.randomUUID());
        item2.setQuantity(3);

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));
        when(invoiceRepository.findBySubscriptionId(any())).thenReturn(Collections.emptyList());
        when(subscriptionItemRepository.findBySubscriptionId(any())).thenReturn(List.of(item1, item2));

        scheduler.generateUpcomingRenewalInvoices();

        verify(invoiceService).createInvoice(any(), argThat(items -> items.size() == 2));
    }

    @Test
    void generateUpcomingRenewalInvoices_ShouldSkipWhenCurrentPeriodEndAfterTargetDate() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodEnd(Instant.now().plus(10, ChronoUnit.DAYS));

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));

        scheduler.generateUpcomingRenewalInvoices();

        verify(invoiceService, never()).createInvoice(any(), any());
    }

    @Test
    void generateUpcomingRenewalInvoices_ShouldSkipWhenInvoiceWithNullPeriodStartExists() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setTenantId(UUID.randomUUID());
        subscription.setCustomerId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodEnd(Instant.now().plus(2, ChronoUnit.DAYS));

        Invoice existingInvoice = new Invoice();
        existingInvoice.setPeriodStart(null);

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));
        when(invoiceRepository.findBySubscriptionId(any())).thenReturn(List.of(existingInvoice));

        scheduler.generateUpcomingRenewalInvoices();

        verify(invoiceService).createInvoice(any(), any());
    }

    @Test
    void detectOverdueInvoices_ShouldHandleNullDueDate() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setDueDate(null);

        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));

        scheduler.detectOverdueInvoices();

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void detectOverdueInvoices_ShouldHandleDueDateEqualToToday() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setDueDate(LocalDate.now());

        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));

        scheduler.detectOverdueInvoices();

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void generateUpcomingRenewalInvoices_ShouldSkipDeletedSubscriptions() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodEnd(Instant.now().plus(2, ChronoUnit.DAYS));
        subscription.setDeletedAt(Instant.now());

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));

        scheduler.generateUpcomingRenewalInvoices();

        verify(invoiceService, never()).createInvoice(any(), any());
    }

    @Test
    void generateUpcomingRenewalInvoices_ShouldSetCorrectInvoiceProperties() {
        UUID subscriptionId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Instant periodEnd = Instant.now().plus(2, ChronoUnit.DAYS);

        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setTenantId(tenantId);
        subscription.setCustomerId(customerId);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodEnd(periodEnd);

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));
        when(invoiceRepository.findBySubscriptionId(any())).thenReturn(Collections.emptyList());
        when(subscriptionItemRepository.findBySubscriptionId(any())).thenReturn(Collections.emptyList());

        scheduler.generateUpcomingRenewalInvoices();

        verify(invoiceService).createInvoice(argThat(invoice -> 
            invoice.getTenantId().equals(tenantId) &&
            invoice.getCustomerId().equals(customerId) &&
            invoice.getSubscriptionId().equals(subscriptionId) &&
            invoice.getStatus() == InvoiceStatus.DRAFT &&
            invoice.getCurrency().equals("USD")
        ), any());
    }

    @Test
    void generateUpcomingRenewalInvoices_ShouldCalculateSubtotalCorrectly() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setTenantId(UUID.randomUUID());
        subscription.setCustomerId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodEnd(Instant.now().plus(2, ChronoUnit.DAYS));

        SubscriptionItem item1 = new SubscriptionItem();
        item1.setId(UUID.randomUUID());
        item1.setQuantity(5);

        SubscriptionItem item2 = new SubscriptionItem();
        item2.setId(UUID.randomUUID());
        item2.setQuantity(3);

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));
        when(invoiceRepository.findBySubscriptionId(any())).thenReturn(Collections.emptyList());
        when(subscriptionItemRepository.findBySubscriptionId(any())).thenReturn(List.of(item1, item2));

        scheduler.generateUpcomingRenewalInvoices();

        verify(invoiceService).createInvoice(argThat(invoice -> 
            invoice.getSubtotal() == 8000L &&
            invoice.getTotal() == 8000L &&
            invoice.getAmountDue() == 8000L
        ), any());
    }

    @Test
    void generateUpcomingRenewalInvoices_ShouldSetInvoiceItemsCorrectly() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setTenantId(UUID.randomUUID());
        subscription.setCustomerId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodEnd(Instant.now().plus(2, ChronoUnit.DAYS));

        SubscriptionItem item = new SubscriptionItem();
        item.setId(UUID.randomUUID());
        item.setQuantity(10);

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));
        when(invoiceRepository.findBySubscriptionId(any())).thenReturn(Collections.emptyList());
        when(subscriptionItemRepository.findBySubscriptionId(any())).thenReturn(List.of(item));

        scheduler.generateUpcomingRenewalInvoices();

        verify(invoiceService).createInvoice(any(), argThat(items -> 
            items.size() == 1 &&
            items.get(0).getQuantity().intValue() == 10 &&
            items.get(0).getUnitAmount() == 1000L &&
            items.get(0).getAmount() == 10000L
        ));
    }

    @Test
    void generateUpcomingRenewalInvoices_ShouldSetPeriodDatesCorrectly() {
        Instant periodEnd = Instant.now().plus(2, ChronoUnit.DAYS);

        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setTenantId(UUID.randomUUID());
        subscription.setCustomerId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodEnd(periodEnd);

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));
        when(invoiceRepository.findBySubscriptionId(any())).thenReturn(Collections.emptyList());
        when(subscriptionItemRepository.findBySubscriptionId(any())).thenReturn(Collections.emptyList());

        scheduler.generateUpcomingRenewalInvoices();

        verify(invoiceService).createInvoice(argThat(invoice -> 
            invoice.getPeriodStart().equals(periodEnd) &&
            invoice.getPeriodEnd().equals(periodEnd.plus(30, ChronoUnit.DAYS)) &&
            invoice.getInvoiceDate().equals(LocalDate.now()) &&
            invoice.getDueDate().equals(LocalDate.now().plusDays(14))
        ), any());
    }

    @Test
    void generateUpcomingRenewalInvoices_WithZeroQuantityItem_ShouldCalculateZeroAmount() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setTenantId(UUID.randomUUID());
        subscription.setCustomerId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodEnd(Instant.now().plus(2, ChronoUnit.DAYS));

        SubscriptionItem item = new SubscriptionItem();
        item.setId(UUID.randomUUID());
        item.setQuantity(0);

        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));
        when(invoiceRepository.findBySubscriptionId(any())).thenReturn(Collections.emptyList());
        when(subscriptionItemRepository.findBySubscriptionId(any())).thenReturn(List.of(item));

        scheduler.generateUpcomingRenewalInvoices();

        verify(invoiceService).createInvoice(argThat(invoice -> 
            invoice.getSubtotal() == 0L &&
            invoice.getTotal() == 0L
        ), any());
    }

    @Test
    void detectOverdueInvoices_ShouldProcessMultipleInvoices() {
        Invoice invoice1 = new Invoice();
        invoice1.setId(UUID.randomUUID());
        invoice1.setStatus(InvoiceStatus.OPEN);
        invoice1.setDueDate(LocalDate.now().minusDays(1));

        Invoice invoice2 = new Invoice();
        invoice2.setId(UUID.randomUUID());
        invoice2.setStatus(InvoiceStatus.OPEN);
        invoice2.setDueDate(LocalDate.now().minusDays(5));

        when(invoiceRepository.findAll()).thenReturn(List.of(invoice1, invoice2));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        scheduler.detectOverdueInvoices();

        verify(invoiceRepository, times(2)).save(argThat(inv -> inv.getStatus() == InvoiceStatus.UNCOLLECTIBLE));
    }

    @Test
    void generateUpcomingRenewalInvoices_WithEmptySubscriptionItems_ShouldCreateInvoiceWithZeroSubtotal() {
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

        verify(invoiceService).createInvoice(argThat(invoice -> 
            invoice.getSubtotal() == 0L &&
            invoice.getTotal() == 0L &&
            invoice.getAmountDue() == 0L
        ), argThat(List::isEmpty));
    }
}

