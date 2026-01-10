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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        invoice.setTotal(BigDecimal.valueOf(100.00));
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
}

