package com.fluxpay.billing.repository;

import com.fluxpay.billing.dto.PaymentFilterDto;
import com.fluxpay.billing.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PaymentRepositoryCustom {
    Page<Payment> findPaymentsWithFilters(UUID tenantId, PaymentFilterDto filters, Pageable pageable);
}

