package com.fluxpay.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarkAllReadResponse {
    private Long updatedCount;
    private String message;
}

