package com.fluxpay.api.controller;

import com.fluxpay.billing.entity.Invoice;
import com.fluxpay.billing.entity.InvoiceItem;
import com.fluxpay.billing.service.InvoiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getInvoice(@PathVariable UUID id) {
        Invoice invoice = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(invoice);
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<List<InvoiceItem>> getInvoiceItems(@PathVariable UUID id) {
        List<InvoiceItem> items = invoiceService.getInvoiceItems(id);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Invoice>> getInvoicesByCustomer(@PathVariable UUID customerId) {
        List<Invoice> invoices = invoiceService.getInvoicesByCustomer(customerId);
        return ResponseEntity.ok(invoices);
    }

    @PostMapping("/{id}/finalize")
    public ResponseEntity<Invoice> finalizeInvoice(@PathVariable UUID id) {
        Invoice invoice = invoiceService.finalizeInvoice(id);
        return ResponseEntity.ok(invoice);
    }

    @PostMapping("/{id}/void")
    public ResponseEntity<Invoice> voidInvoice(@PathVariable UUID id) {
        Invoice invoice = invoiceService.voidInvoice(id);
        return ResponseEntity.ok(invoice);
    }
}

