package com.fluxpay.billing.service;

import com.fluxpay.billing.entity.Invoice;
import com.fluxpay.billing.entity.InvoiceItem;
import com.fluxpay.billing.repository.InvoiceItemRepository;
import com.fluxpay.billing.repository.InvoiceRepository;
import com.fluxpay.billing.service.TaxService;
import com.fluxpay.common.enums.InvoiceStatus;
import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.security.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceItemRepository invoiceItemRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    private Invoice invoice;
    private UUID tenantId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);
        
        customerId = UUID.randomUUID();
        invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setCustomerId(customerId);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setSubtotal(100L);
        invoice.setTotal(100L);
        invoice.setAmountDue(100L);
        invoice.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getInvoiceById_Success() {
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        Invoice result = invoiceService.getInvoiceById(invoice.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(invoice.getId());
    }

    @Test
    void getInvoiceById_ThrowsException_WhenNotFound() {
        UUID id = UUID.randomUUID();
        when(invoiceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoiceById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getInvoicesByCustomer_Success() {
        List<Invoice> invoices = Arrays.asList(invoice);
        when(invoiceRepository.findByTenantIdAndCustomerId(tenantId, customerId)).thenReturn(invoices);

        List<Invoice> result = invoiceService.getInvoicesByCustomer(customerId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerId()).isEqualTo(customerId);
    }

    @Test
    void createInvoice_ShouldGenerateInvoiceNumber() {
        Invoice newInvoice = new Invoice();
        newInvoice.setCustomerId(customerId);
        newInvoice.setSubtotal(100L);
        newInvoice.setTotal(100L);

        List<InvoiceItem> items = Arrays.asList(new InvoiceItem());

        when(invoiceRepository.findTopByTenantIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(invoiceItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Invoice result = invoiceService.createInvoice(newInvoice, items);

        assertThat(result.getInvoiceNumber()).isNotNull();
        verify(invoiceRepository).save(any(Invoice.class));
        verify(invoiceItemRepository).save(any(InvoiceItem.class));
    }

    @Test
    void createInvoiceWithTax_ShouldCalculateTax() {
        Invoice newInvoice = new Invoice();
        newInvoice.setCustomerId(customerId);
        newInvoice.setSubtotal(10000L);
        newInvoice.setTotal(10000L);

        List<InvoiceItem> items = Arrays.asList(new InvoiceItem());

        TaxService taxService = mock(TaxService.class);
        InvoiceService service = new InvoiceService(invoiceRepository, invoiceItemRepository, taxService);

        when(taxService.calculateTax(anyLong(), anyString())).thenReturn(java.util.Map.of("taxAmount", 1000L, "taxRate", 0.10));
        when(invoiceRepository.findTopByTenantIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(invoiceItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Invoice result = service.createInvoiceWithTax(newInvoice, items, "US");

        assertThat(result.getTax()).isEqualTo(1000L);
        assertThat(result.getTotal()).isEqualTo(11000L);
        verify(taxService).calculateTax(10000L, "US");
    }

    @Test
    void finalizeInvoice_ShouldSetStatusToOpen() {
        invoice.setStatus(InvoiceStatus.DRAFT);
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Invoice result = invoiceService.finalizeInvoice(invoice.getId());

        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.OPEN);
    }

    @Test
    void markInvoiceAsPaid_ShouldUpdateStatusAndAmounts() {
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Invoice result = invoiceService.markInvoiceAsPaid(invoice.getId());

        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(result.getPaidAt()).isNotNull();
        assertThat(result.getAmountPaid()).isEqualTo(invoice.getTotal());
        assertThat(result.getAmountDue()).isEqualTo(0L);
    }

    @Test
    void voidInvoice_ShouldSetStatusToVoid() {
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Invoice result = invoiceService.voidInvoice(invoice.getId());

        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.VOID);
    }

    @Test
    void getInvoiceItems_ShouldReturnItems() {
        UUID invoiceId = invoice.getId();
        InvoiceItem item = new InvoiceItem();
        item.setInvoiceId(invoiceId);

        when(invoiceItemRepository.findByInvoiceId(invoiceId)).thenReturn(Arrays.asList(item));

        List<InvoiceItem> result = invoiceService.getInvoiceItems(invoiceId);

        assertThat(result).hasSize(1);
    }

    @Test
    void getInvoicesBySubscription_ShouldReturnInvoices() {
        UUID subscriptionId = UUID.randomUUID();
        invoice.setSubscriptionId(subscriptionId);

        when(invoiceRepository.findBySubscriptionId(subscriptionId)).thenReturn(Arrays.asList(invoice));

        List<Invoice> result = invoiceService.getInvoicesBySubscription(subscriptionId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSubscriptionId()).isEqualTo(subscriptionId);
    }

    @Test
    void getInvoiceById_ShouldThrowWhenDeleted() {
        invoice.setDeletedAt(java.time.Instant.now());
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.getInvoiceById(invoice.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

