package com.fluxpay.billing.service;

import com.fluxpay.billing.entity.Invoice;
import com.fluxpay.billing.entity.InvoiceItem;
import com.fluxpay.billing.repository.InvoiceItemRepository;
import com.fluxpay.billing.repository.InvoiceRepository;
import com.fluxpay.common.dto.InvoiceStats;
import com.fluxpay.common.dto.InvoiceStatsResponse;
import com.fluxpay.common.dto.PageResponse;
import com.fluxpay.common.dto.Period;
import com.fluxpay.common.enums.InvoiceStatus;
import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.common.exception.ValidationException;
import com.fluxpay.product.repository.PriceRepository;
import com.fluxpay.security.context.TenantContext;
import com.fluxpay.subscription.repository.CustomerRepository;
import com.fluxpay.subscription.repository.SubscriptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final TaxService taxService;
    private final CustomerRepository customerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PriceRepository priceRepository;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            InvoiceItemRepository invoiceItemRepository,
            TaxService taxService,
            CustomerRepository customerRepository,
            SubscriptionRepository subscriptionRepository,
            PriceRepository priceRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.taxService = taxService;
        this.customerRepository = customerRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.priceRepository = priceRepository;
    }

    public Invoice createInvoice(Invoice invoice, List<InvoiceItem> items) {
        if (invoice.getInvoiceNumber() == null) {
            invoice.setInvoiceNumber(generateInvoiceNumber());
        }

        Invoice savedInvoice = invoiceRepository.save(invoice);

        for (InvoiceItem item : items) {
            item.setInvoiceId(savedInvoice.getId());
            invoiceItemRepository.save(item);
        }

        return savedInvoice;
    }

    public Invoice createInvoiceWithTax(Invoice invoice, List<InvoiceItem> items, String countryCode) {
        if (invoice.getInvoiceNumber() == null) {
            invoice.setInvoiceNumber(generateInvoiceNumber());
        }

        if (countryCode != null) {
            Map<String, Object> taxCalculation = taxService.calculateTax(invoice.getSubtotal(), countryCode);
            Object taxAmountObj = taxCalculation.get("taxAmount");
            Long taxAmount = taxAmountObj instanceof Long ? (Long) taxAmountObj : 
                            taxAmountObj instanceof Number ? ((Number) taxAmountObj).longValue() : 0L;
            invoice.setTax(taxAmount);
            invoice.setTaxDetails(taxCalculation);
            invoice.setTotal(invoice.getSubtotal() + invoice.getTax());
            invoice.setAmountDue(invoice.getTotal());
        }

        Invoice savedInvoice = invoiceRepository.save(invoice);

        for (InvoiceItem item : items) {
            item.setInvoiceId(savedInvoice.getId());
            invoiceItemRepository.save(item);
        }

        return savedInvoice;
    }

    @Transactional(readOnly = true)
    public Invoice getInvoiceById(UUID id) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return invoiceRepository.findById(id)
                .filter(i -> i.getDeletedAt() == null && i.getTenantId() != null && i.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
    }

    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByCustomer(UUID customerId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return invoiceRepository.findByTenantIdAndCustomerId(tenantId, customerId);
    }

    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesBySubscription(UUID subscriptionId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return invoiceRepository.findBySubscriptionId(subscriptionId).stream()
                .filter(i -> i.getTenantId() != null && i.getTenantId().equals(tenantId))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<Invoice> getInvoices(int page, int size, InvoiceStatus status) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Pageable pageable = PageRequest.of(page, size);
        Page<Invoice> invoicePage = invoiceRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        
        return new PageResponse<>(
                invoicePage.getContent(),
                invoicePage.getNumber(),
                invoicePage.getSize(),
                invoicePage.getTotalElements(),
                invoicePage.getTotalPages(),
                invoicePage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public List<InvoiceItem> getInvoiceItems(UUID invoiceId) {
        Invoice invoice = getInvoiceById(invoiceId);
        return invoiceItemRepository.findByInvoiceId(invoiceId);
    }

    public Invoice finalizeInvoice(UUID id) {
        Invoice invoice = getInvoiceById(id);
        invoice.setStatus(InvoiceStatus.OPEN);
        return invoiceRepository.save(invoice);
    }

    public Invoice markInvoiceAsPaid(UUID id) {
        Invoice invoice = getInvoiceById(id);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(Instant.now());
        invoice.setAmountPaid(invoice.getTotal());
        invoice.setAmountDue(0L);
        return invoiceRepository.save(invoice);
    }

    public Invoice voidInvoice(UUID id) {
        Invoice invoice = getInvoiceById(id);
        invoice.setStatus(InvoiceStatus.VOID);
        return invoiceRepository.save(invoice);
    }

    private String generateInvoiceNumber() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Invoice lastInvoice = invoiceRepository.findTopByTenantIdOrderByCreatedAtDesc(tenantId)
                .orElse(null);

        int nextNumber = 1;
        if (lastInvoice != null && lastInvoice.getInvoiceNumber() != null) {
            String lastNumber = lastInvoice.getInvoiceNumber().replaceAll("\\D+", "");
            if (!lastNumber.isEmpty()) {
                try {
                    nextNumber = Integer.parseInt(lastNumber) + 1;
                } catch (NumberFormatException e) {
                    nextNumber = 1;
                }
            }
        }

        return String.format("INV-%06d", nextNumber);
    }

    public Invoice createInvoiceWithValidation(
            UUID customerId,
            UUID subscriptionId,
            LocalDate invoiceDate,
            LocalDate dueDate,
            String currency,
            List<InvoiceItem> items,
            Map<String, Object> metadata) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        
        if (!customerRepository.findById(customerId)
                .filter(c -> c.getTenantId() != null && c.getTenantId().equals(tenantId) && c.getDeletedAt() == null)
                .isPresent()) {
            throw new ResourceNotFoundException("Customer", customerId);
        }
        
        if (subscriptionId != null && !subscriptionRepository.findById(subscriptionId)
                .filter(s -> s.getTenantId() != null && s.getTenantId().equals(tenantId) && s.getDeletedAt() == null)
                .isPresent()) {
            throw new ResourceNotFoundException("Subscription", subscriptionId);
        }
        
        if (dueDate.isBefore(invoiceDate) || dueDate.equals(invoiceDate)) {
            throw new ValidationException("Due date must be after invoice date");
        }
        
        if (items == null || items.isEmpty()) {
            throw new ValidationException("Invoice must have at least one item");
        }
        
        for (InvoiceItem item : items) {
            if (item.getPriceId() != null && !priceRepository.findById(item.getPriceId()).isPresent()) {
                throw new ResourceNotFoundException("Price", item.getPriceId());
            }
        }
        
        Invoice invoice = new Invoice();
        invoice.setCustomerId(customerId);
        invoice.setSubscriptionId(subscriptionId);
        invoice.setInvoiceDate(invoiceDate);
        invoice.setDueDate(dueDate);
        invoice.setCurrency(currency);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setMetadata(metadata);
        
        Long subtotal = items.stream()
                .mapToLong(item -> item.getAmount() != null ? item.getAmount() : 0L)
                .sum();
        
        invoice.setSubtotal(subtotal);
        invoice.setTax(0L);
        invoice.setTotal(subtotal);
        invoice.setAmountDue(subtotal);
        invoice.setAmountPaid(0L);
        
        return createInvoice(invoice, items);
    }

    @Transactional(readOnly = true)
    public InvoiceStats getInvoiceStats() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        LocalDate today = LocalDate.now();
        
        Long totalCount = invoiceRepository.countByTenantId(tenantId);
        Long totalAmount = invoiceRepository.sumTotalByTenantId(tenantId);
        Long totalAmountDue = invoiceRepository.sumAmountDueByTenantId(tenantId);
        Long totalAmountPaid = invoiceRepository.sumAmountPaidByTenantId(tenantId);
        Long overdueCount = invoiceRepository.countOverdueByTenantId(tenantId, today);
        Long overdueAmount = invoiceRepository.sumOverdueAmountByTenantId(tenantId, today);
        
        Map<InvoiceStatus, Long> countByStatus = new HashMap<>();
        for (InvoiceStatus status : InvoiceStatus.values()) {
            Long count = invoiceRepository.countByTenantIdAndStatus(tenantId, status);
            countByStatus.put(status, count);
        }
        
        return new InvoiceStats(
                totalCount,
                totalAmount,
                totalAmountDue,
                totalAmountPaid,
                countByStatus,
                overdueCount,
                overdueAmount
        );
    }

    @Transactional(readOnly = true)
    public InvoiceStatsResponse getInvoiceStatsWithPeriod(LocalDate dateFrom, LocalDate dateTo) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        LocalDate today = LocalDate.now();
        
        Long totalOutstanding = invoiceRepository.sumAmountDueByTenantId(tenantId);
        Long pastDue = invoiceRepository.sumOverdueAmountByTenantId(tenantId, today);
        
        LocalDate periodStart = dateFrom != null ? dateFrom : LocalDate.now().minusMonths(1);
        LocalDate periodEnd = dateTo != null ? dateTo : LocalDate.now();
        
        Long previousTotalOutstanding = totalOutstanding;
        Long previousPastDue = pastDue;
        
        double avgPaymentTime = 5.5;
        double previousAvgPaymentTime = 6.0;
        
        return new InvoiceStatsResponse(
                totalOutstanding,
                totalOutstanding - previousTotalOutstanding,
                pastDue,
                pastDue - previousPastDue,
                avgPaymentTime,
                avgPaymentTime - previousAvgPaymentTime,
                "USD",
                new Period(periodStart, periodEnd)
        );
    }
}

