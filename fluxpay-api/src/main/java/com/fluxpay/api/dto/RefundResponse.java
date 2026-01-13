package com.fluxpay.api.dto;

import com.fluxpay.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {
    private UUID id;
    private UUID paymentId;
    private Long amount;
    private String currency;
    private PaymentStatus status;
    private String reason;
    private String refundId;
    private Instant createdAt;
}

