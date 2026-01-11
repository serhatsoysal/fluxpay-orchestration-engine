package com.fluxpay.billing.service;

import com.fluxpay.billing.entity.ExchangeRate;
import com.fluxpay.billing.repository.ExchangeRateRepository;
import com.fluxpay.common.enums.Currency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @InjectMocks
    private CurrencyService currencyService;

    @Test
    void convertAmount_SameCurrency_ShouldReturnSameAmount() {
        Long amount = 10000L;
        Long result = currencyService.convertAmount(amount, Currency.USD, Currency.USD);
        assertThat(result).isEqualTo(amount);
    }

    @Test
    void convertAmount_DifferentCurrency_ShouldConvertCorrectly() {
        ExchangeRate rate = new ExchangeRate();
        rate.setId(UUID.randomUUID());
        rate.setFromCurrency(Currency.USD);
        rate.setToCurrency(Currency.EUR);
        rate.setRate(BigDecimal.valueOf(0.85));
        rate.setEffectiveDate(LocalDate.now());
        rate.setActive(true);

        when(exchangeRateRepository.findTopByFromCurrencyAndToCurrencyAndActiveTrueAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                any(), any(), any())).thenReturn(Optional.of(rate));

        Long result = currencyService.convertAmount(10000L, Currency.USD, Currency.EUR);
        assertThat(result).isEqualTo(8500L);
    }

    @Test
    void convertAmount_NoExchangeRate_ShouldThrowException() {
        when(exchangeRateRepository.findTopByFromCurrencyAndToCurrencyAndActiveTrueAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                any(), any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> currencyService.convertAmount(10000L, Currency.USD, Currency.EUR))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Exchange rate not found");
    }
}

