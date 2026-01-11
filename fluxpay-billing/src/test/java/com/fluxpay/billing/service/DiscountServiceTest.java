package com.fluxpay.billing.service;

import com.fluxpay.billing.entity.Coupon;
import com.fluxpay.billing.entity.Discount;
import com.fluxpay.billing.repository.CouponRepository;
import com.fluxpay.billing.repository.DiscountRepository;
import com.fluxpay.common.enums.DiscountType;
import com.fluxpay.security.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private DiscountRepository discountRepository;

    @InjectMocks
    private DiscountService discountService;

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
    void calculateDiscount_PercentageType_ShouldCalculateCorrectly() {
        Coupon coupon = new Coupon();
        coupon.setId(UUID.randomUUID());
        coupon.setCode("SAVE20");
        coupon.setDiscountType(DiscountType.PERCENTAGE);
        coupon.setDiscountValue(BigDecimal.valueOf(20));
        coupon.setActive(true);

        when(couponRepository.findByTenantIdAndCode(any(), any())).thenReturn(Optional.of(coupon));

        Long discount = discountService.calculateDiscount(10000L, "SAVE20");
        assertThat(discount).isEqualTo(2000L);
    }

    @Test
    void calculateDiscount_FixedAmountType_ShouldCalculateCorrectly() {
        Coupon coupon = new Coupon();
        coupon.setId(UUID.randomUUID());
        coupon.setCode("SAVE10");
        coupon.setDiscountType(DiscountType.FIXED_AMOUNT);
        coupon.setDiscountValue(BigDecimal.valueOf(10));
        coupon.setActive(true);

        when(couponRepository.findByTenantIdAndCode(any(), any())).thenReturn(Optional.of(coupon));

        Long discount = discountService.calculateDiscount(10000L, "SAVE10");
        assertThat(discount).isEqualTo(1000L);
    }

    @Test
    void applyDiscount_ShouldIncrementCouponUsage() {
        Coupon coupon = new Coupon();
        coupon.setId(UUID.randomUUID());
        coupon.setCode("SAVE20");
        coupon.setDiscountType(DiscountType.PERCENTAGE);
        coupon.setDiscountValue(BigDecimal.valueOf(20));
        coupon.setActive(true);
        coupon.setTimesRedeemed(0);

        when(couponRepository.findByTenantIdAndCode(any(), any())).thenReturn(Optional.of(coupon));
        when(discountRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        discountService.applyDiscount(UUID.randomUUID(), UUID.randomUUID(), "SAVE20", 10000L);

        verify(couponRepository).save(argThat(c -> c.getTimesRedeemed() == 1));
        verify(discountRepository).save(any(Discount.class));
    }
}

