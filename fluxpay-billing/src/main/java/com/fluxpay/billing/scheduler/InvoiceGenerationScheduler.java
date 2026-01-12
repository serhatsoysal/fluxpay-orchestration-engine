package com.fluxpay.billing.scheduler;

import com.fluxpay.billing.entity.Invoice;
import com.fluxpay.billing.entity.InvoiceItem;
import com.fluxpay.billing.repository.InvoiceRepository;
import com.fluxpay.billing.service.InvoiceService;
import com.fluxpay.common.enums.InvoiceStatus;
import com.fluxpay.common.enums.SubscriptionStatus;
import com.fluxpay.subscription.entity.Subscription;
import com.fluxpay.subscription.entity.SubscriptionItem;
import com.fluxpay.subscription.repository.SubscriptionItemRepository;
import com.fluxpay.subscription.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
public class InvoiceGenerationScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionItemRepository subscriptionItemRepository;
    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;

    @Value("${INVOICE_GENERATION_DAYS_AHEAD:3}")
    private int invoiceGenerationDaysAhead;

    @Value("${INVOICE_DUE_DAYS:14}")
    private int invoiceDueDays;

    @Value("${INVOICE_PERIOD_DAYS:30}")
    private int invoicePeriodDays;

    @Value("${INVOICE_DEFAULT_CURRENCY:USD}")
    private String invoiceDefaultCurrency;

    @Value("${INVOICE_ITEM_UNIT_AMOUNT:1000}")
    private long invoiceItemUnitAmount;

    public InvoiceGenerationScheduler(
            SubscriptionRepository subscriptionRepository,
            SubscriptionItemRepository subscriptionItemRepository,
            InvoiceService invoiceService,
            InvoiceRepository invoiceRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionItemRepository = subscriptionItemRepository;
        this.invoiceService = invoiceService;
        this.invoiceRepository = invoiceRepository;
    }

    @Scheduled(cron = "${INVOICE_GENERATION_CRON:0 0 1 * * ?}")
    @Transactional
    public void generateUpcomingRenewalInvoices() {
        Instant targetDate = Instant.now().plus(invoiceGenerationDaysAhead, ChronoUnit.DAYS);

        List<Subscription> upcomingRenewals = subscriptionRepository.findAll().stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .filter(s -> s.getCurrentPeriodEnd().isBefore(targetDate))
                .filter(s -> s.getDeletedAt() == null)
                .toList();

        for (Subscription subscription : upcomingRenewals) {
            boolean invoiceExists = invoiceRepository.findBySubscriptionId(subscription.getId()).stream()
                    .anyMatch(inv -> inv.getPeriodStart() != null && 
                             inv.getPeriodStart().equals(subscription.getCurrentPeriodEnd()));

            if (!invoiceExists) {
                generateInvoiceForSubscription(subscription);
            }
        }
    }

    @Scheduled(cron = "${INVOICE_OVERDUE_DETECTION_CRON:0 30 1 * * ?}")
    @Transactional
    public void detectOverdueInvoices() {
        LocalDate today = LocalDate.now();

        List<Invoice> openInvoices = invoiceRepository.findAll().stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.OPEN)
                .filter(inv -> inv.getDueDate() != null && inv.getDueDate().isBefore(today))
                .filter(inv -> inv.getDeletedAt() == null)
                .toList();

        for (Invoice invoice : openInvoices) {
            invoice.setStatus(InvoiceStatus.UNCOLLECTIBLE);
            invoiceRepository.save(invoice);
        }
    }

    private void generateInvoiceForSubscription(Subscription subscription) {
        List<SubscriptionItem> items = subscriptionItemRepository.findBySubscriptionId(subscription.getId());

        Invoice invoice = new Invoice();
        invoice.setTenantId(subscription.getTenantId());
        invoice.setCustomerId(subscription.getCustomerId());
        invoice.setSubscriptionId(subscription.getId());
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setCurrency(invoiceDefaultCurrency);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(invoiceDueDays));
        invoice.setPeriodStart(subscription.getCurrentPeriodEnd());
        invoice.setPeriodEnd(subscription.getCurrentPeriodEnd().plus(invoicePeriodDays, ChronoUnit.DAYS));

        long subtotal = 0L;
        List<InvoiceItem> invoiceItems = new ArrayList<>();

        for (SubscriptionItem subItem : items) {
            InvoiceItem invoiceItem = new InvoiceItem();
            invoiceItem.setDescription("Subscription item");
            invoiceItem.setQuantity(BigDecimal.valueOf(subItem.getQuantity()));
            invoiceItem.setUnitAmount(invoiceItemUnitAmount);
            invoiceItem.setAmount((long) subItem.getQuantity() * invoiceItemUnitAmount);
            
            subtotal += invoiceItem.getAmount();
            invoiceItems.add(invoiceItem);
        }

        invoice.setSubtotal(subtotal);
        invoice.setTotal(subtotal);
        invoice.setAmountDue(subtotal);

        invoiceService.createInvoice(invoice, invoiceItems);
    }
}

