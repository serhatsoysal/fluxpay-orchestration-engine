package com.fluxpay.billing.repository;

import com.fluxpay.billing.entity.ExchangeRate;
import com.fluxpay.common.enums.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, UUID> {
    Optional<ExchangeRate> findTopByFromCurrencyAndToCurrencyAndActiveTrueAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
            Currency fromCurrency, Currency toCurrency, LocalDate effectiveDate);
}

