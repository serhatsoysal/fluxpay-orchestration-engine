package com.fluxpay.api.controller;

import com.fluxpay.billing.entity.Invoice;
import com.fluxpay.billing.entity.InvoiceItem;
import com.fluxpay.billing.service.InvoiceService;
import com.fluxpay.common.dto.InvoiceStats;
import com.fluxpay.common.dto.PageResponse;
import com.fluxpay.common.enums.InvoiceStatus;
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

    @GetMapping
    public ResponseEntity<PageResponse<Invoice>> getInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) InvoiceStatus status) {
        PageResponse<Invoice> response = invoiceService.getInvoices(page, size, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<InvoiceStats> getInvoiceStats() {
        InvoiceStats stats = invoiceService.getInvoiceStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Invoice>> getInvoicesByCustomer(@PathVariable UUID customerId) {
        List<Invoice> invoices = invoiceService.getInvoicesByCustomer(customerId);
        return ResponseEntity.ok(invoices);
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

