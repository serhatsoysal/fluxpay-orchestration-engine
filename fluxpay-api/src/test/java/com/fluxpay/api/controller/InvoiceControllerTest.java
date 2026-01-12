package com.fluxpay.api.controller;

import com.fluxpay.billing.entity.Invoice;
import com.fluxpay.billing.entity.InvoiceItem;
import com.fluxpay.billing.service.InvoiceService;
import com.fluxpay.common.enums.InvoiceStatus;
import com.fluxpay.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceControllerTest {

    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private InvoiceController invoiceController;

    private Invoice invoice;
    private UUID invoiceId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        invoiceId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setCustomerId(customerId);
        invoice.setInvoiceNumber("INV-000001");
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setSubtotal(10000L);
        invoice.setTotal(10000L);
        invoice.setAmountDue(10000L);
        invoice.setCurrency("USD");
    }

    @Test
    void getInvoice_Success() {
        when(invoiceService.getInvoiceById(invoiceId)).thenReturn(invoice);

        ResponseEntity<Invoice> response = invoiceController.getInvoice(invoiceId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(invoiceId);
        assertThat(response.getBody().getInvoiceNumber()).isEqualTo("INV-000001");
        verify(invoiceService).getInvoiceById(invoiceId);
    }

    @Test
    void getInvoice_NotFound_ThrowsException() {
        when(invoiceService.getInvoiceById(invoiceId)).thenThrow(new ResourceNotFoundException("Invoice", invoiceId));

        assertThatThrownBy(() -> invoiceController.getInvoice(invoiceId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(invoiceService).getInvoiceById(invoiceId);
    }

    @Test
    void getInvoiceItems_Success() {
        InvoiceItem item1 = new InvoiceItem();
        item1.setId(UUID.randomUUID());
        item1.setInvoiceId(invoiceId);
        item1.setAmount(5000L);

        InvoiceItem item2 = new InvoiceItem();
        item2.setId(UUID.randomUUID());
        item2.setInvoiceId(invoiceId);
        item2.setAmount(5000L);

        List<InvoiceItem> items = Arrays.asList(item1, item2);
        when(invoiceService.getInvoiceItems(invoiceId)).thenReturn(items);

        ResponseEntity<List<InvoiceItem>> response = invoiceController.getInvoiceItems(invoiceId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getInvoiceId()).isEqualTo(invoiceId);
        verify(invoiceService).getInvoiceItems(invoiceId);
    }

    @Test
    void getInvoiceItems_ReturnsEmptyList() {
        when(invoiceService.getInvoiceItems(invoiceId)).thenReturn(Collections.emptyList());

        ResponseEntity<List<InvoiceItem>> response = invoiceController.getInvoiceItems(invoiceId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();
        verify(invoiceService).getInvoiceItems(invoiceId);
    }

    @Test
    void getInvoicesByCustomer_Success() {
        Invoice invoice2 = new Invoice();
        invoice2.setId(UUID.randomUUID());
        invoice2.setCustomerId(customerId);
        invoice2.setInvoiceNumber("INV-000002");

        List<Invoice> invoices = Arrays.asList(invoice, invoice2);
        when(invoiceService.getInvoicesByCustomer(customerId)).thenReturn(invoices);

        ResponseEntity<List<Invoice>> response = invoiceController.getInvoicesByCustomer(customerId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getCustomerId()).isEqualTo(customerId);
        assertThat(response.getBody().get(1).getCustomerId()).isEqualTo(customerId);
        verify(invoiceService).getInvoicesByCustomer(customerId);
    }

    @Test
    void getInvoicesByCustomer_ReturnsEmptyList() {
        when(invoiceService.getInvoicesByCustomer(customerId)).thenReturn(Collections.emptyList());

        ResponseEntity<List<Invoice>> response = invoiceController.getInvoicesByCustomer(customerId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();
        verify(invoiceService).getInvoicesByCustomer(customerId);
    }

    @Test
    void finalizeInvoice_Success() {
        Invoice finalizedInvoice = new Invoice();
        finalizedInvoice.setId(invoiceId);
        finalizedInvoice.setStatus(InvoiceStatus.OPEN);

        when(invoiceService.finalizeInvoice(invoiceId)).thenReturn(finalizedInvoice);

        ResponseEntity<Invoice> response = invoiceController.finalizeInvoice(invoiceId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(invoiceId);
        assertThat(response.getBody().getStatus()).isEqualTo(InvoiceStatus.OPEN);
        verify(invoiceService).finalizeInvoice(invoiceId);
    }

    @Test
    void finalizeInvoice_NotFound_ThrowsException() {
        when(invoiceService.finalizeInvoice(invoiceId)).thenThrow(new ResourceNotFoundException("Invoice", invoiceId));

        assertThatThrownBy(() -> invoiceController.finalizeInvoice(invoiceId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(invoiceService).finalizeInvoice(invoiceId);
    }

    @Test
    void voidInvoice_Success() {
        Invoice voidedInvoice = new Invoice();
        voidedInvoice.setId(invoiceId);
        voidedInvoice.setStatus(InvoiceStatus.VOID);

        when(invoiceService.voidInvoice(invoiceId)).thenReturn(voidedInvoice);

        ResponseEntity<Invoice> response = invoiceController.voidInvoice(invoiceId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(invoiceId);
        assertThat(response.getBody().getStatus()).isEqualTo(InvoiceStatus.VOID);
        verify(invoiceService).voidInvoice(invoiceId);
    }

    @Test
    void voidInvoice_NotFound_ThrowsException() {
        when(invoiceService.voidInvoice(invoiceId)).thenThrow(new ResourceNotFoundException("Invoice", invoiceId));

        assertThatThrownBy(() -> invoiceController.voidInvoice(invoiceId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(invoiceService).voidInvoice(invoiceId);
    }
}

