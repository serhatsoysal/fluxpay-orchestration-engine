package com.fluxpay.billing.service;

import com.fluxpay.billing.entity.ExchangeRate;
import com.fluxpay.billing.repository.ExchangeRateRepository;
import com.fluxpay.common.enums.Currency;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@Transactional
public class CurrencyService {

    private final ExchangeRateRepository exchangeRateRepository;

    public CurrencyService(ExchangeRateRepository exchangeRateRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
    }

    public Long convertAmount(Long amount, Currency fromCurrency, Currency toCurrency) {
        if (fromCurrency == toCurrency) {
            return amount;
        }

        ExchangeRate rate = exchangeRateRepository
                .findTopByFromCurrencyAndToCurrencyAndActiveTrueAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                        fromCurrency, toCurrency, LocalDate.now())
                .orElseThrow(() -> new RuntimeException("Exchange rate not found: " + fromCurrency + " to " + toCurrency));

        BigDecimal amountDecimal = BigDecimal.valueOf(amount);
        BigDecimal convertedAmount = amountDecimal.multiply(rate.getRate()).setScale(0, RoundingMode.HALF_UP);

        return convertedAmount.longValue();
    }

    public ExchangeRate createExchangeRate(ExchangeRate exchangeRate) {
        return exchangeRateRepository.save(exchangeRate);
    }
}

