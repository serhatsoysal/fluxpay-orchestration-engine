package com.fluxpay.billing.dto;

import com.fluxpay.common.enums.PaymentMethod;
import com.fluxpay.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentQueryParams {
    private int page;
    private int size;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private UUID invoiceId;
    private UUID customerId;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private Long amountMin;
    private Long amountMax;
}

