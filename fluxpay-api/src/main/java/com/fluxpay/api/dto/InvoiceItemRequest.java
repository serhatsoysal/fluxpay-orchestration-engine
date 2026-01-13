package com.fluxpay.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class InvoiceItemRequest {
    private UUID priceId;

    @NotNull
    private String description;

    @NotNull
    @Min(1)
    private Integer quantity;

    @NotNull
    @Min(0)
    private Long unitAmount;

    @NotNull
    @Min(0)
    private Long amount;

    @NotNull
    private Boolean isProration = false;
}

