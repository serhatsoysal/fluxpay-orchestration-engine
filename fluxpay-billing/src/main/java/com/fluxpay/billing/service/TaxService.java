package com.fluxpay.billing.service;

import com.fluxpay.billing.entity.TaxRate;
import com.fluxpay.billing.repository.TaxRateRepository;
import com.fluxpay.security.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class TaxService {

    private final TaxRateRepository taxRateRepository;

    public TaxService(TaxRateRepository taxRateRepository) {
        this.taxRateRepository = taxRateRepository;
    }

    public Map<String, Object> calculateTax(Long subtotal, String countryCode) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        TaxRate taxRate = taxRateRepository
                .findByTenantIdAndCountryCodeAndActiveTrueOrderByCreatedAtDesc(tenantId, countryCode)
                .orElse(null);

        Map<String, Object> result = new HashMap<>();
        
        if (taxRate == null) {
            result.put("taxAmount", 0L);
            result.put("taxRate", BigDecimal.ZERO);
            result.put("taxType", "NONE");
            return result;
        }

        BigDecimal subtotalDecimal = BigDecimal.valueOf(subtotal).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal taxAmountDecimal = subtotalDecimal.multiply(taxRate.getPercentage()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        Long taxAmount = taxAmountDecimal.multiply(BigDecimal.valueOf(100)).longValue();

        result.put("taxAmount", taxAmount);
        result.put("taxRate", taxRate.getPercentage());
        result.put("taxType", taxRate.getTaxType().name());
        result.put("taxRateId", taxRate.getId());
        result.put("taxRateName", taxRate.getName());

        return result;
    }

    public TaxRate createTaxRate(TaxRate taxRate) {
        return taxRateRepository.save(taxRate);
    }
}

