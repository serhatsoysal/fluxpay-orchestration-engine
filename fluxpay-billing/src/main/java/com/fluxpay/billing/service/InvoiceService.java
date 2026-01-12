package com.fluxpay.billing.service;

import com.fluxpay.billing.entity.Invoice;
import com.fluxpay.billing.entity.InvoiceItem;
import com.fluxpay.billing.repository.InvoiceItemRepository;
import com.fluxpay.billing.repository.InvoiceRepository;
import com.fluxpay.common.dto.InvoiceStats;
import com.fluxpay.common.dto.PageResponse;
import com.fluxpay.common.enums.InvoiceStatus;
import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.security.context.TenantContext;
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

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            InvoiceItemRepository invoiceItemRepository,
            TaxService taxService) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.taxService = taxService;
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
            invoice.setTax((Long) taxCalculation.get("taxAmount"));
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
        return invoiceRepository.findById(id)
                .filter(i -> i.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
    }

    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByCustomer(UUID customerId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return invoiceRepository.findByTenantIdAndCustomerId(tenantId, customerId);
    }

    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesBySubscription(UUID subscriptionId) {
        return invoiceRepository.findBySubscriptionId(subscriptionId);
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
        if (lastInvoice != null) {
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
}

