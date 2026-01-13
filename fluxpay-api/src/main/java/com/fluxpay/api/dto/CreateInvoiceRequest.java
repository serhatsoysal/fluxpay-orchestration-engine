package com.fluxpay.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class CreateInvoiceRequest {
    @NotNull
    private UUID customerId;

    private UUID subscriptionId;

    @NotNull
    private LocalDate invoiceDate;

    @NotNull
    private LocalDate dueDate;

    @NotNull
    private String currency;

    @NotEmpty
    @Valid
    private List<InvoiceItemRequest> items;

    private Map<String, Object> metadata;
}

