package com.fluxpay.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceStatsResponse {
    private Long totalOutstanding;
    private Long totalOutstandingChange;
    private Long pastDue;
    private Long pastDueChange;
    private Double avgPaymentTime;
    private Double avgPaymentTimeChange;
    private String currency;
    private Period period;
}

