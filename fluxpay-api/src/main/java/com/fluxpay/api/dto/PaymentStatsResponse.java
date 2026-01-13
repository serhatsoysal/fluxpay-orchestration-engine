package com.fluxpay.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private com.fluxpay.common.dto.Period period;
}
