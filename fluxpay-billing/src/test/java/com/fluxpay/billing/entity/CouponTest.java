package com.fluxpay.billing.entity;

import com.fluxpay.common.enums.DiscountType;
import com.fluxpay.security.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CouponTest {

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
    void prePersist_ShouldSetTenantId() {
        Coupon coupon = new Coupon();
        coupon.setCode("TEST");
        coupon.setDiscountType(DiscountType.PERCENTAGE);
        coupon.setDiscountValue(BigDecimal.valueOf(10));

        coupon.prePersist();

        assertThat(coupon.getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void isValid_ShouldReturnFalseWhenInactive() {
        Coupon coupon = new Coupon();
        coupon.setActive(false);

        assertThat(coupon.isValid()).isFalse();
    }

    @Test
    void isValid_ShouldReturnFalseWhenValidFromNotReached() {
        Coupon coupon = new Coupon();
        coupon.setActive(true);
        coupon.setValidFrom(Instant.now().plusSeconds(3600));

        assertThat(coupon.isValid()).isFalse();
    }

    @Test
    void isValid_ShouldReturnFalseWhenValidUntilPassed() {
        Coupon coupon = new Coupon();
        coupon.setActive(true);
        coupon.setValidUntil(Instant.now().minusSeconds(3600));

        assertThat(coupon.isValid()).isFalse();
    }

    @Test
    void isValid_ShouldReturnFalseWhenMaxRedemptionsReached() {
        Coupon coupon = new Coupon();
        coupon.setActive(true);
        coupon.setMaxRedemptions(5);
        coupon.setTimesRedeemed(5);

        assertThat(coupon.isValid()).isFalse();
    }

    @Test
    void isValid_ShouldReturnTrueWhenValid() {
        Coupon coupon = new Coupon();
        coupon.setActive(true);
        coupon.setMaxRedemptions(5);
        coupon.setTimesRedeemed(3);

        assertThat(coupon.isValid()).isTrue();
    }

    @Test
    void isValid_ShouldReturnTrueWhenNoRestrictions() {
        Coupon coupon = new Coupon();
        coupon.setActive(true);

        assertThat(coupon.isValid()).isTrue();
    }
}

