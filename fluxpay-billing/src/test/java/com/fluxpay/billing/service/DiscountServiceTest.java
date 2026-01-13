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

    @Test
    void calculateDiscount_ShouldThrowWhenCouponNotFound() {
        when(couponRepository.findByTenantIdAndCode(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> discountService.calculateDiscount(10000L, "INVALID"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Coupon not found");
    }

    @Test
    void calculateDiscount_ShouldThrowWhenCouponInvalid() {
        Coupon coupon = new Coupon();
        coupon.setId(UUID.randomUUID());
        coupon.setCode("EXPIRED");
        coupon.setDiscountType(DiscountType.PERCENTAGE);
        coupon.setDiscountValue(BigDecimal.valueOf(20));
        coupon.setActive(false);

        when(couponRepository.findByTenantIdAndCode(any(), any())).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> discountService.calculateDiscount(10000L, "EXPIRED"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not valid");
    }

    @Test
    void calculateDiscount_FixedAmount_ShouldNotExceedAmount() {
        Coupon coupon = new Coupon();
        coupon.setId(UUID.randomUUID());
        coupon.setCode("SAVE100");
        coupon.setDiscountType(DiscountType.FIXED_AMOUNT);
        coupon.setDiscountValue(BigDecimal.valueOf(100));
        coupon.setActive(true);

        when(couponRepository.findByTenantIdAndCode(any(), any())).thenReturn(Optional.of(coupon));

        Long discount = discountService.calculateDiscount(5000L, "SAVE100");
        assertThat(discount).isEqualTo(5000L);
    }

    @Test
    void createCoupon_ShouldSaveCoupon() {
        Coupon coupon = new Coupon();
        coupon.setCode("NEWCOUPON");
        coupon.setDiscountType(DiscountType.PERCENTAGE);
        coupon.setDiscountValue(BigDecimal.valueOf(15));

        when(couponRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Coupon saved = discountService.createCoupon(coupon);

        verify(couponRepository).save(coupon);
        assertThat(saved).isEqualTo(coupon);
    }

    @Test
    void calculateDiscount_Percentage_ShouldHandleZeroAmount() {
        Coupon coupon = new Coupon();
        coupon.setId(UUID.randomUUID());
        coupon.setCode("SAVE20");
        coupon.setDiscountType(DiscountType.PERCENTAGE);
        coupon.setDiscountValue(BigDecimal.valueOf(20));
        coupon.setActive(true);

        when(couponRepository.findByTenantIdAndCode(any(), any())).thenReturn(Optional.of(coupon));

        Long discount = discountService.calculateDiscount(0L, "SAVE20");
        assertThat(discount).isZero();
    }

    @Test
    void calculateDiscount_Percentage_Should100PercentDiscount() {
        Coupon coupon = new Coupon();
        coupon.setId(UUID.randomUUID());
        coupon.setCode("FREE");
        coupon.setDiscountType(DiscountType.PERCENTAGE);
        coupon.setDiscountValue(BigDecimal.valueOf(100));
        coupon.setActive(true);

        when(couponRepository.findByTenantIdAndCode(any(), any())).thenReturn(Optional.of(coupon));

        Long discount = discountService.calculateDiscount(10000L, "FREE");
        assertThat(discount).isEqualTo(10000L);
    }

    @Test
    void applyDiscount_ShouldThrowWhenCouponNotFound() {
        when(couponRepository.findByTenantIdAndCode(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> discountService.applyDiscount(UUID.randomUUID(), UUID.randomUUID(), "NOTFOUND", 10000L))
                .isInstanceOf(com.fluxpay.common.exception.ResourceNotFoundException.class)
                .hasMessageContaining("Coupon");
    }

    @Test
    void calculateDiscount_FixedAmount_ShouldHandleZeroAmount() {
        Coupon coupon = new Coupon();
        coupon.setId(UUID.randomUUID());
        coupon.setCode("SAVE10");
        coupon.setDiscountType(DiscountType.FIXED_AMOUNT);
        coupon.setDiscountValue(BigDecimal.valueOf(10));
        coupon.setActive(true);

        when(couponRepository.findByTenantIdAndCode(any(), any())).thenReturn(Optional.of(coupon));

        Long discount = discountService.calculateDiscount(0L, "SAVE10");
        assertThat(discount).isZero();
    }

    @Test
    void calculateDiscount_FixedAmount_ShouldHandleExactMatch() {
        Coupon coupon = new Coupon();
        coupon.setId(UUID.randomUUID());
        coupon.setCode("SAVE10");
        coupon.setDiscountType(DiscountType.FIXED_AMOUNT);
        coupon.setDiscountValue(BigDecimal.valueOf(10));
        coupon.setActive(true);

        when(couponRepository.findByTenantIdAndCode(any(), any())).thenReturn(Optional.of(coupon));

        Long discount = discountService.calculateDiscount(1000L, "SAVE10");
        assertThat(discount).isEqualTo(1000L);
    }

    @Test
    void applyDiscount_ShouldIncrementMultipleTimes() {
        Coupon coupon = new Coupon();
        coupon.setId(UUID.randomUUID());
        coupon.setCode("SAVE20");
        coupon.setDiscountType(DiscountType.PERCENTAGE);
        coupon.setDiscountValue(BigDecimal.valueOf(20));
        coupon.setActive(true);
        coupon.setTimesRedeemed(5);

        when(couponRepository.findByTenantIdAndCode(any(), any())).thenReturn(Optional.of(coupon));
        when(discountRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        discountService.applyDiscount(UUID.randomUUID(), UUID.randomUUID(), "SAVE20", 10000L);

        verify(couponRepository).save(argThat(c -> c.getTimesRedeemed() == 6));
    }

    @Test
    void calculateDiscount_Percentage_ShouldRoundCorrectly() {
        Coupon coupon = new Coupon();
        coupon.setId(UUID.randomUUID());
        coupon.setCode("SAVE33");
        coupon.setDiscountType(DiscountType.PERCENTAGE);
        coupon.setDiscountValue(BigDecimal.valueOf(33.33));
        coupon.setActive(true);

        when(couponRepository.findByTenantIdAndCode(any(), any())).thenReturn(Optional.of(coupon));

        Long discount = discountService.calculateDiscount(10000L, "SAVE33");
        assertThat(discount).isEqualTo(3333L);
    }
}

