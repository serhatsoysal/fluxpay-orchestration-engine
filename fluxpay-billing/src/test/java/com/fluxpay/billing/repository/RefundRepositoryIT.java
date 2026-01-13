package com.fluxpay.billing.repository;

import com.fluxpay.billing.entity.Payment;
import com.fluxpay.billing.entity.Refund;
import com.fluxpay.common.enums.PaymentMethod;
import com.fluxpay.common.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Transactional
class RefundRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private UUID tenantId;
    private UUID paymentId;
    private Payment payment;

    @BeforeEach
    void setUp() {
        refundRepository.deleteAll();
        paymentRepository.deleteAll();

        tenantId = UUID.randomUUID();
        paymentId = UUID.randomUUID();

        payment = new Payment();
        payment.setTenantId(tenantId);
        payment.setCustomerId(UUID.randomUUID());
        payment.setAmount(10000L);
        payment.setCurrency("USD");
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        payment.setRefundedAmount(0L);
        payment = paymentRepository.save(payment);
        paymentId = payment.getId();
    }

    @Test
    void findByPaymentId_ShouldReturnRefundsForPayment() {
        Refund refund1 = createRefund(tenantId, paymentId, 5000L, PaymentStatus.COMPLETED);
        Refund refund2 = createRefund(tenantId, paymentId, 3000L, PaymentStatus.PENDING);
        Refund refund3 = createRefund(tenantId, UUID.randomUUID(), 2000L, PaymentStatus.COMPLETED);

        List<Refund> saved = refundRepository.saveAll(List.of(refund1, refund2, refund3));
        refundRepository.flush();

        List<Refund> refunds = refundRepository.findByPaymentId(paymentId);

        assertThat(refunds)
                .hasSize(2)
                .allMatch(r -> r.getPaymentId().equals(paymentId))
                .extracting(Refund::getId)
                .containsExactlyInAnyOrder(saved.get(0).getId(), saved.get(1).getId());
    }

    @Test
    void findByPaymentId_WithNoRefunds_ShouldReturnEmptyList() {
        List<Refund> refunds = refundRepository.findByPaymentId(paymentId);

        assertThat(refunds).isEmpty();
    }

    @Test
    void save_ShouldPersistRefundWithAllFields() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reason_code", "customer_request");
        metadata.put("processed_by", "admin@example.com");

        Refund refund = createRefund(tenantId, paymentId, 5000L, PaymentStatus.COMPLETED);
        refund.setReason("Customer requested refund");
        refund.setRefundId("ref_123456");
        refund.setMetadata(metadata);
        refund = refundRepository.save(refund);
        refundRepository.flush();

        Refund saved = refundRepository.findById(refund.getId()).orElseThrow();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTenantId()).isEqualTo(tenantId);
        assertThat(saved.getPaymentId()).isEqualTo(paymentId);
        assertThat(saved.getAmount()).isEqualTo(5000L);
        assertThat(saved.getCurrency()).isEqualTo("USD");
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(saved.getReason()).isEqualTo("Customer requested refund");
        assertThat(saved.getRefundId()).isEqualTo("ref_123456");
        assertThat(saved.getMetadata()).isEqualTo(metadata);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void save_ShouldAutoSetTenantIdFromContext() {
        com.fluxpay.security.context.TenantContext.setCurrentTenant(tenantId);

        Refund refund = new Refund();
        refund.setPaymentId(paymentId);
        refund.setAmount(1000L);
        refund.setCurrency("USD");
        refund.setStatus(PaymentStatus.PENDING);

        refund = refundRepository.save(refund);
        refundRepository.flush();

        Refund saved = refundRepository.findById(refund.getId()).orElseThrow();
        assertThat(saved.getTenantId()).isEqualTo(tenantId);

        com.fluxpay.security.context.TenantContext.clear();
    }

    @Test
    void delete_ShouldRemoveRefund() {
        Refund refund = createRefund(tenantId, paymentId, 5000L, PaymentStatus.COMPLETED);
        refund = refundRepository.save(refund);
        refundRepository.flush();

        UUID refundId = refund.getId();
        refundRepository.delete(refund);
        refundRepository.flush();

        Optional<Refund> deleted = refundRepository.findById(refundId);
        assertThat(deleted).isEmpty();
    }

    private Refund createRefund(UUID tenantId, UUID paymentId, Long amount, PaymentStatus status) {
        Refund refund = new Refund();
        refund.setTenantId(tenantId);
        refund.setPaymentId(paymentId);
        refund.setAmount(amount);
        refund.setCurrency("USD");
        refund.setStatus(status);
        return refund;
    }
}

