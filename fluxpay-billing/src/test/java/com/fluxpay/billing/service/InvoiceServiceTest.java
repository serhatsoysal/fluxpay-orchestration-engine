package com.fluxpay.billing.service;

import com.fluxpay.billing.entity.Invoice;
import com.fluxpay.billing.repository.InvoiceRepository;
import com.fluxpay.common.enums.InvoiceStatus;
import com.fluxpay.common.exception.ResourceNotFoundException;
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

    @InjectMocks
    private InvoiceService invoiceService;

    private Invoice invoice;

    @BeforeEach
    void setUp() {
        invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setCustomerId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setTotal(BigDecimal.valueOf(100.00));
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
        when(invoiceRepository.findByCustomerId(invoice.getCustomerId())).thenReturn(invoices);

        List<Invoice> result = invoiceService.getInvoicesByCustomer(invoice.getCustomerId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerId()).isEqualTo(invoice.getCustomerId());
    }
}

