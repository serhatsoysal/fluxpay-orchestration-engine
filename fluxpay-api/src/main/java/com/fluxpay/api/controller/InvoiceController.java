package com.fluxpay.api.controller;

import com.fluxpay.api.dto.CreateInvoiceRequest;
import com.fluxpay.api.dto.InvoiceItemRequest;
import com.fluxpay.billing.entity.Invoice;
import com.fluxpay.billing.entity.InvoiceItem;
import com.fluxpay.billing.service.InvoiceService;
import com.fluxpay.common.dto.InvoiceStatsResponse;
import com.fluxpay.common.dto.PageResponse;
import com.fluxpay.common.enums.InvoiceStatus;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    public ResponseEntity<Invoice> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        List<InvoiceItem> items = new ArrayList<>();
        for (InvoiceItemRequest itemRequest : request.getItems()) {
            InvoiceItem item = new InvoiceItem();
            item.setPriceId(itemRequest.getPriceId());
            item.setDescription(itemRequest.getDescription());
            item.setQuantity(BigDecimal.valueOf(itemRequest.getQuantity()));
            item.setUnitAmount(itemRequest.getUnitAmount());
            item.setAmount(itemRequest.getAmount());
            item.setIsProration(itemRequest.getIsProration());
            items.add(item);
        }
        
        Invoice invoice = invoiceService.createInvoiceWithValidation(
                request.getCustomerId(),
                request.getSubscriptionId(),
                request.getInvoiceDate(),
                request.getDueDate(),
                request.getCurrency(),
                items,
                request.getMetadata());
        return ResponseEntity.status(HttpStatus.CREATED).body(invoice);
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
    public ResponseEntity<InvoiceStatsResponse> getInvoiceStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        InvoiceStatsResponse stats = invoiceService.getInvoiceStatsWithPeriod(dateFrom, dateTo);
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

