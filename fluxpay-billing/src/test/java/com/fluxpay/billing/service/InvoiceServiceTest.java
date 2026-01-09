package com.fluxpay.billing.service;

import com.fluxpay.billing.entity.Invoice;
import com.fluxpay.billing.entity.InvoiceItem;
import com.fluxpay.billing.repository.InvoiceItemRepository;
import com.fluxpay.billing.repository.InvoiceRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    private UUID invoiceId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        invoiceId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);
        
        invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setTenantId(tenantId);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setTotal(10000L);
        invoice.setAmountDue(10000L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createInvoice_ShouldGenerateInvoiceNumber() {
        InvoiceItem item = new InvoiceItem();
        item.setId(UUID.randomUUID());

        when(invoiceRepository.findTopByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);
        when(invoiceItemRepository.save(any(InvoiceItem.class))).thenReturn(item);

        Invoice result = invoiceService.createInvoice(invoice, List.of(item));

        assertThat(result).isEqualTo(invoice);
        assertThat(invoice.getInvoiceNumber()).isEqualTo("INV-000001");
        verify(invoiceItemRepository).save(item);
    }

    @Test
    void createInvoice_ShouldUseExistingInvoiceNumber() {
        invoice.setInvoiceNumber("INV-123456");
        InvoiceItem item = new InvoiceItem();
        
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);
        when(invoiceItemRepository.save(any(InvoiceItem.class))).thenReturn(item);

        Invoice result = invoiceService.createInvoice(invoice, List.of(item));

        assertThat(result.getInvoiceNumber()).isEqualTo("INV-123456");
    }

    @Test
    void getInvoiceById_ShouldReturnInvoice() {
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        Invoice result = invoiceService.getInvoiceById(invoiceId);

        assertThat(result).isEqualTo(invoice);
    }

    @Test
    void getInvoiceById_ShouldThrowException_WhenNotFound() {
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoiceById(invoiceId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void finalizeInvoice_ShouldSucceed() {
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        Invoice result = invoiceService.finalizeInvoice(invoiceId);

        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.OPEN);
    }

    @Test
    void markInvoiceAsPaid_ShouldSucceed() {
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        Invoice result = invoiceService.markInvoiceAsPaid(invoiceId);

        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(result.getPaidAt()).isNotNull();
        assertThat(result.getAmountPaid()).isEqualTo(10000L);
        assertThat(result.getAmountDue()).isZero();
    }

    @Test
    void voidInvoice_ShouldSucceed() {
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        Invoice result = invoiceService.voidInvoice(invoiceId);

        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.VOID);
    }
}

