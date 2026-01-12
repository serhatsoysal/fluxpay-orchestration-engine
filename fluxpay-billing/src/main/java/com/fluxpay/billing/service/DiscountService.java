package com.fluxpay.billing.service;

import com.fluxpay.billing.entity.Coupon;
import com.fluxpay.billing.entity.Discount;
import com.fluxpay.billing.repository.CouponRepository;
import com.fluxpay.billing.repository.DiscountRepository;
import com.fluxpay.common.enums.DiscountType;
import com.fluxpay.security.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@Transactional
public class DiscountService {

    private final CouponRepository couponRepository;
    private final DiscountRepository discountRepository;

    public DiscountService(CouponRepository couponRepository, DiscountRepository discountRepository) {
        this.couponRepository = couponRepository;
        this.discountRepository = discountRepository;
    }

    public Long calculateDiscount(Long amount, String couponCode) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Coupon coupon = couponRepository.findByTenantIdAndCode(tenantId, couponCode)
                .orElseThrow(() -> new RuntimeException("Coupon not found: " + couponCode));

        if (!coupon.isValid()) {
            throw new RuntimeException("Coupon is not valid");
        }

        if (coupon.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            long discountAmount = coupon.getDiscountValue()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();
            return Math.min(discountAmount, amount);
        } else {
            BigDecimal amountDecimal = BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal discountDecimal = amountDecimal.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            return discountDecimal.multiply(BigDecimal.valueOf(100)).longValue();
        }
    }

    public Discount applyDiscount(UUID subscriptionId, UUID invoiceId, String couponCode, Long amount) {
        Long discountAmount = calculateDiscount(amount, couponCode);

        UUID tenantId = TenantContext.getCurrentTenantId();
        Coupon coupon = couponRepository.findByTenantIdAndCode(tenantId, couponCode)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));

        coupon.setTimesRedeemed(coupon.getTimesRedeemed() + 1);
        couponRepository.save(coupon);

        Discount discount = new Discount();
        discount.setCouponId(coupon.getId());
        discount.setSubscriptionId(subscriptionId);
        discount.setInvoiceId(invoiceId);
        discount.setDiscountAmount(discountAmount);

        return discountRepository.save(discount);
    }

    public Coupon createCoupon(Coupon coupon) {
        return couponRepository.save(coupon);
    }
}

