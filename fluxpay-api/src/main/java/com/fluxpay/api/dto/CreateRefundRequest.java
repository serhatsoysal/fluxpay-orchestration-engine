package com.fluxpay.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class CreateRefundRequest {
    @NotNull
    @Min(1)
    private Long amount;

    private String reason;

    private Map<String, Object> metadata;
}

