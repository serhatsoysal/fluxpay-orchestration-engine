package com.fluxpay.billing.service;

import com.fluxpay.billing.entity.TaxRate;
import com.fluxpay.billing.repository.TaxRateRepository;
import com.fluxpay.common.enums.TaxType;
import com.fluxpay.security.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxServiceTest {

    @Mock
    private TaxRateRepository taxRateRepository;

    @InjectMocks
    private TaxService taxService;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void calculateTax_WithValidTaxRate_ShouldReturnCorrectTaxAmount() {
        TaxRate taxRate = new TaxRate();
        taxRate.setId(UUID.randomUUID());
        taxRate.setTenantId(tenantId);
        taxRate.setName("VAT");
        taxRate.setTaxType(TaxType.VAT);
        taxRate.setPercentage(BigDecimal.valueOf(20));
        taxRate.setCountryCode("US");
        taxRate.setActive(true);

        when(taxRateRepository.findByTenantIdAndCountryCodeAndActiveTrueOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(taxRate));

        Map<String, Object> result = taxService.calculateTax(10000L, "US");

        assertThat(result.get("taxAmount")).isEqualTo(2000L);
        assertThat(result.get("taxRate")).isEqualTo(BigDecimal.valueOf(20));
        assertThat(result.get("taxType")).isEqualTo("VAT");
    }

    @Test
    void calculateTax_WithNoTaxRate_ShouldReturnZeroTax() {
        when(taxRateRepository.findByTenantIdAndCountryCodeAndActiveTrueOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());

        Map<String, Object> result = taxService.calculateTax(10000L, "US");

        assertThat(result.get("taxAmount")).isEqualTo(0L);
        assertThat(result.get("taxRate")).isEqualTo(BigDecimal.ZERO);
        assertThat(result.get("taxType")).isEqualTo("NONE");
    }
}

