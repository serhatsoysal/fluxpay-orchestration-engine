package com.fluxpay.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatsResponse {
    private Long totalRevenue;
    private Long totalCount;
    private Long completedCount;
    private Long failedCount;
    private Long pendingCount;
    private Long refundedAmount;
    private Long averagePaymentAmount;
    private String currency;
    private Period period;
}

