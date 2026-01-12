package com.fluxpay.common.dto;

import com.fluxpay.common.enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceStats {
    private Long totalCount;
    private Long totalAmount;
    private Long totalAmountDue;
    private Long totalAmountPaid;
    private Map<InvoiceStatus, Long> countByStatus;
    private Long overdueCount;
    private Long overdueAmount;
}

