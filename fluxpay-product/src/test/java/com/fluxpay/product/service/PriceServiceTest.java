package com.fluxpay.product.service;

import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.product.entity.Price;
import com.fluxpay.product.repository.PriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceServiceTest {

    @Mock
    private PriceRepository priceRepository;

    @InjectMocks
    private PriceService priceService;

    private Price price;
    private UUID priceId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        priceId = UUID.randomUUID();
        productId = UUID.randomUUID();

        price = new Price();
        price.setId(priceId);
        price.setProductId(productId);
        price.setUnitAmount(new BigDecimal("10000"));
        price.setCurrency("USD");
        price.setActive(true);
        price.setDeletedAt(null);
    }

    @Test
    void createPrice_ShouldReturnSavedPrice() {
        Price newPrice = new Price();
        newPrice.setProductId(productId);
        newPrice.setUnitAmount(new BigDecimal("5000"));
        newPrice.setCurrency("EUR");

        when(priceRepository.save(any(Price.class))).thenAnswer(invocation -> {
            Price p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        Price result = priceService.createPrice(newPrice);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        verify(priceRepository).save(newPrice);
    }

    @Test
    void getPriceById_ShouldReturnPrice() {
        when(priceRepository.findById(priceId)).thenReturn(Optional.of(price));

        Price result = priceService.getPriceById(priceId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(priceId);
        verify(priceRepository).findById(priceId);
    }

    @Test
    void getPriceById_WhenNotFound_ShouldThrowException() {
        when(priceRepository.findById(priceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> priceService.getPriceById(priceId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(priceRepository).findById(priceId);
    }

    @Test
    void getPriceById_WhenDeleted_ShouldThrowException() {
        price.setDeletedAt(java.time.Instant.now());
        when(priceRepository.findById(priceId)).thenReturn(Optional.of(price));

        assertThatThrownBy(() -> priceService.getPriceById(priceId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(priceRepository).findById(priceId);
    }

    @Test
    void getActivePricesByProduct_ShouldReturnActivePrices() {
        Price activePrice1 = new Price();
        activePrice1.setId(UUID.randomUUID());
        activePrice1.setActive(true);
        Price activePrice2 = new Price();
        activePrice2.setId(UUID.randomUUID());
        activePrice2.setActive(true);

        List<Price> activePrices = Arrays.asList(activePrice1, activePrice2);

        when(priceRepository.findByProductIdAndActive(productId, true)).thenReturn(activePrices);

        List<Price> result = priceService.getActivePricesByProduct(productId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(Price::getActive);
        verify(priceRepository).findByProductIdAndActive(productId, true);
    }

    @Test
    void getActivePricesByProduct_ShouldReturnEmptyList() {
        when(priceRepository.findByProductIdAndActive(productId, true)).thenReturn(List.of());

        List<Price> result = priceService.getActivePricesByProduct(productId);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(priceRepository).findByProductIdAndActive(productId, true);
    }

    @Test
    void getAllPricesByProduct_ShouldReturnAllPrices() {
        Price price1 = new Price();
        price1.setId(UUID.randomUUID());
        price1.setActive(true);
        Price price2 = new Price();
        price2.setId(UUID.randomUUID());
        price2.setActive(false);

        List<Price> allPrices = Arrays.asList(price1, price2);

        when(priceRepository.findByProductId(productId)).thenReturn(allPrices);

        List<Price> result = priceService.getAllPricesByProduct(productId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(priceRepository).findByProductId(productId);
    }

    @Test
    void updatePrice_ShouldUpdateFields() {
        Price updatedPrice = new Price();
        updatedPrice.setUnitAmount(new BigDecimal("15000"));
        updatedPrice.setCurrency("EUR");
        updatedPrice.setTiers(null);
        updatedPrice.setTrialPeriodDays(14);
        updatedPrice.setActive(false);

        when(priceRepository.findById(priceId)).thenReturn(Optional.of(price));
        when(priceRepository.save(any(Price.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Price result = priceService.updatePrice(priceId, updatedPrice);

        assertThat(result).isNotNull();
        assertThat(result.getUnitAmount()).isEqualByComparingTo(new BigDecimal("15000"));
        assertThat(result.getCurrency()).isEqualTo("EUR");
        assertThat(result.getTrialPeriodDays()).isEqualTo(14);
        assertThat(result.getActive()).isFalse();
        verify(priceRepository).save(price);
    }

    @Test
    void updatePrice_WhenNotFound_ShouldThrowException() {
        Price updatedPrice = new Price();
        updatedPrice.setUnitAmount(new BigDecimal("15000"));

        when(priceRepository.findById(priceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> priceService.updatePrice(priceId, updatedPrice))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(priceRepository, never()).save(any());
    }

    @Test
    void deactivatePrice_ShouldSetActiveToFalse() {
        price.setActive(true);

        when(priceRepository.findById(priceId)).thenReturn(Optional.of(price));
        when(priceRepository.save(any(Price.class))).thenAnswer(invocation -> invocation.getArgument(0));

        priceService.deactivatePrice(priceId);

        assertThat(price.getActive()).isFalse();
        verify(priceRepository).findById(priceId);
        verify(priceRepository).save(price);
    }

    @Test
    void deactivatePrice_WhenNotFound_ShouldThrowException() {
        when(priceRepository.findById(priceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> priceService.deactivatePrice(priceId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(priceRepository, never()).save(any());
    }
}

