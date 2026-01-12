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
import static org.mockito.Mockito.verify;
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

        assertThat(result).containsEntry("taxAmount", 2000L);
        assertThat(result).containsEntry("taxRate", BigDecimal.valueOf(20));
        assertThat(result).containsEntry("taxType", "VAT");
    }

    @Test
    void calculateTax_WithNoTaxRate_ShouldReturnZeroTax() {
        when(taxRateRepository.findByTenantIdAndCountryCodeAndActiveTrueOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());

        Map<String, Object> result = taxService.calculateTax(10000L, "US");

        assertThat(result).containsEntry("taxAmount", 0L);
        assertThat(result).containsEntry("taxRate", BigDecimal.ZERO);
        assertThat(result).containsEntry("taxType", "NONE");
    }

    @Test
    void createTaxRate_ShouldSaveTaxRate() {
        TaxRate taxRate = new TaxRate();
        taxRate.setTenantId(tenantId);
        taxRate.setName("Sales Tax");
        taxRate.setTaxType(TaxType.SALES_TAX);
        taxRate.setPercentage(BigDecimal.valueOf(8.5));
        taxRate.setCountryCode("US");
        taxRate.setActive(true);

        when(taxRateRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TaxRate result = taxService.createTaxRate(taxRate);

        verify(taxRateRepository).save(taxRate);
        assertThat(result).isEqualTo(taxRate);
    }

    @Test
    void calculateTax_WithZeroAmount_ShouldReturnZeroTax() {
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

        Map<String, Object> result = taxService.calculateTax(0L, "US");

        assertThat(result).containsEntry("taxAmount", 0L);
        assertThat(result).containsEntry("taxRate", BigDecimal.valueOf(20));
        assertThat(result).containsEntry("taxType", "VAT");
    }

    @Test
    void calculateTax_WithLargeAmount_ShouldCalculateCorrectly() {
        TaxRate taxRate = new TaxRate();
        taxRate.setId(UUID.randomUUID());
        taxRate.setTenantId(tenantId);
        taxRate.setName("VAT");
        taxRate.setTaxType(TaxType.VAT);
        taxRate.setPercentage(BigDecimal.valueOf(15));
        taxRate.setCountryCode("US");
        taxRate.setActive(true);

        when(taxRateRepository.findByTenantIdAndCountryCodeAndActiveTrueOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(taxRate));

        Map<String, Object> result = taxService.calculateTax(1000000L, "US");

        assertThat(result).containsEntry("taxAmount", 150000L);
        assertThat(result).containsEntry("taxRate", BigDecimal.valueOf(15));
    }

    @Test
    void calculateTax_WithDecimalPercentage_ShouldRoundCorrectly() {
        TaxRate taxRate = new TaxRate();
        taxRate.setId(UUID.randomUUID());
        taxRate.setTenantId(tenantId);
        taxRate.setName("Custom Tax");
        taxRate.setTaxType(TaxType.SALES_TAX);
        taxRate.setPercentage(BigDecimal.valueOf(7.75));
        taxRate.setCountryCode("US");
        taxRate.setActive(true);

        when(taxRateRepository.findByTenantIdAndCountryCodeAndActiveTrueOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(taxRate));

        Map<String, Object> result = taxService.calculateTax(10000L, "US");

        assertThat(result).containsEntry("taxAmount", 775L);
        assertThat(result).containsEntry("taxRate", BigDecimal.valueOf(7.75));
    }

    @Test
    void calculateTax_ShouldIncludeAllResultFields() {
        UUID taxRateId = UUID.randomUUID();
        TaxRate taxRate = new TaxRate();
        taxRate.setId(taxRateId);
        taxRate.setTenantId(tenantId);
        taxRate.setName("State Tax");
        taxRate.setTaxType(TaxType.SALES_TAX);
        taxRate.setPercentage(BigDecimal.valueOf(10));
        taxRate.setCountryCode("US");
        taxRate.setActive(true);

        when(taxRateRepository.findByTenantIdAndCountryCodeAndActiveTrueOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(taxRate));

        Map<String, Object> result = taxService.calculateTax(10000L, "US");

        assertThat(result).containsEntry("taxAmount", 1000L);
        assertThat(result).containsEntry("taxRate", BigDecimal.valueOf(10));
        assertThat(result).containsEntry("taxType", "SALES_TAX");
        assertThat(result).containsEntry("taxRateId", taxRateId);
        assertThat(result).containsEntry("taxRateName", "State Tax");
    }

    @Test
    void calculateTax_WithHighPercentage_ShouldCalculateCorrectly() {
        TaxRate taxRate = new TaxRate();
        taxRate.setId(UUID.randomUUID());
        taxRate.setTenantId(tenantId);
        taxRate.setName("High Tax");
        taxRate.setTaxType(TaxType.VAT);
        taxRate.setPercentage(BigDecimal.valueOf(25));
        taxRate.setCountryCode("SE");
        taxRate.setActive(true);

        when(taxRateRepository.findByTenantIdAndCountryCodeAndActiveTrueOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(taxRate));

        Map<String, Object> result = taxService.calculateTax(10000L, "SE");

        assertThat(result).containsEntry("taxAmount", 2500L);
        assertThat(result).containsEntry("taxRate", BigDecimal.valueOf(25));
    }

    @Test
    void calculateTax_WithComplexRounding_ShouldRoundCorrectly() {
        TaxRate taxRate = new TaxRate();
        taxRate.setId(UUID.randomUUID());
        taxRate.setTenantId(tenantId);
        taxRate.setName("Complex Tax");
        taxRate.setTaxType(TaxType.VAT);
        taxRate.setPercentage(BigDecimal.valueOf(13.33));
        taxRate.setCountryCode("US");
        taxRate.setActive(true);

        when(taxRateRepository.findByTenantIdAndCountryCodeAndActiveTrueOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(taxRate));

        Map<String, Object> result = taxService.calculateTax(9999L, "US");

        assertThat(result).containsEntry("taxAmount", 1332L);
    }
}

