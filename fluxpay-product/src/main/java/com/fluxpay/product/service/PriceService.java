package com.fluxpay.product.service;

import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.product.entity.Price;
import com.fluxpay.product.repository.PriceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PriceService {

    private final PriceRepository priceRepository;

    public PriceService(PriceRepository priceRepository) {
        this.priceRepository = priceRepository;
    }

    public Price createPrice(Price price) {
        return priceRepository.save(price);
    }

    @Transactional(readOnly = true)
    public Price getPriceById(UUID id) {
        return priceRepository.findById(id)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Price", id));
    }

    @Transactional(readOnly = true)
    public List<Price> getActivePricesByProduct(UUID productId) {
        return priceRepository.findByProductIdAndActive(productId, true);
    }

    @Transactional(readOnly = true)
    public List<Price> getAllPricesByProduct(UUID productId) {
        return priceRepository.findByProductId(productId);
    }

    public Price updatePrice(UUID id, Price updatedPrice) {
        Price price = getPriceById(id);
        
        price.setUnitAmount(updatedPrice.getUnitAmount());
        price.setCurrency(updatedPrice.getCurrency());
        price.setTiers(updatedPrice.getTiers());
        price.setTrialPeriodDays(updatedPrice.getTrialPeriodDays());
        price.setActive(updatedPrice.getActive());

        return priceRepository.save(price);
    }

    public void deactivatePrice(UUID id) {
        Price price = getPriceById(id);
        price.setActive(false);
        priceRepository.save(price);
    }
}

