package com.fluxpay.billing.repository;

import com.fluxpay.billing.dto.PaymentFilterDto;
import com.fluxpay.billing.entity.Payment;
import com.fluxpay.common.enums.PaymentMethod;
import com.fluxpay.common.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Transactional
class PaymentRepositoryIT {

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
    private PaymentRepository paymentRepository;

    private UUID tenantId1;
    private UUID tenantId2;
    private UUID customerId1;
    private UUID customerId2;
    private UUID invoiceId1;
    private UUID invoiceId2;
    private Payment payment1;
    private Payment payment2;
    private Payment payment3;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();

        tenantId1 = UUID.randomUUID();
        tenantId2 = UUID.randomUUID();
        customerId1 = UUID.randomUUID();
        customerId2 = UUID.randomUUID();
        invoiceId1 = UUID.randomUUID();
        invoiceId2 = UUID.randomUUID();

        payment1 = createPayment(tenantId1, customerId1, invoiceId1, PaymentStatus.COMPLETED, PaymentMethod.CREDIT_CARD, 10000L, Instant.now().minusSeconds(3600));
        payment2 = createPayment(tenantId1, customerId1, invoiceId1, PaymentStatus.PENDING, PaymentMethod.DEBIT_CARD, 20000L, Instant.now().minusSeconds(1800));
        payment3 = createPayment(tenantId1, customerId2, invoiceId2, PaymentStatus.COMPLETED, PaymentMethod.CREDIT_CARD, 15000L, Instant.now());

        Payment payment4 = createPayment(tenantId2, customerId1, null, PaymentStatus.COMPLETED, PaymentMethod.PAYPAL, 5000L, Instant.now());

        paymentRepository.saveAll(List.of(payment1, payment2, payment3, payment4));
    }

    private Payment createPayment(UUID tenantId, UUID customerId, UUID invoiceId, PaymentStatus status, PaymentMethod method, Long amount, Instant createdAt) {
        Payment payment = new Payment();
        payment.setTenantId(tenantId);
        payment.setCustomerId(customerId);
        payment.setInvoiceId(invoiceId);
        payment.setAmount(amount);
        payment.setCurrency("USD");
        payment.setStatus(status);
        payment.setPaymentMethod(method);
        payment.setRefundedAmount(0L);
        payment.setCreatedAt(createdAt);
        return payment;
    }

    @Test
    void findByTenantIdAndCustomerId_ShouldReturnOnlyTenantPayments() {
        List<Payment> payments = paymentRepository.findByTenantIdAndCustomerId(tenantId1, customerId1);

        assertThat(payments)
                .hasSize(2)
                .allMatch(p -> p.getTenantId().equals(tenantId1))
                .allMatch(p -> p.getCustomerId().equals(customerId1));
    }

    @Test
    void findByInvoiceId_ShouldReturnAllPaymentsForInvoice() {
        List<Payment> payments = paymentRepository.findByInvoiceId(invoiceId1);

        assertThat(payments)
                .hasSize(2)
                .allMatch(p -> p.getInvoiceId().equals(invoiceId1));
    }

    @Test
    void findByStatus_ShouldReturnPaymentsByStatus() {
        List<Payment> payments = paymentRepository.findByStatus(PaymentStatus.COMPLETED);

        assertThat(payments)
                .hasSize(3)
                .allMatch(p -> p.getStatus().equals(PaymentStatus.COMPLETED));
    }

    @Test
    void findPaymentsWithFilters_ShouldFilterByAllCriteria() {
        Pageable pageable = PageRequest.of(0, 10);
        PaymentFilterDto filters = PaymentFilterDto.builder()
                .status(PaymentStatus.COMPLETED)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .invoiceId(invoiceId2)
                .customerId(customerId2)
                .dateFrom(java.time.LocalDate.now().minusDays(1))
                .dateTo(java.time.LocalDate.now())
                .amountMin(10000L)
                .amountMax(20000L)
                .build();

        Page<Payment> result = paymentRepository.findPaymentsWithFilters(tenantId1, filters, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(payment3);
    }

    @Test
    void findPaymentsWithFilters_WithPagination_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 1);
        PaymentFilterDto filters = PaymentFilterDto.builder().build();

        Page<Payment> result = paymentRepository.findPaymentsWithFilters(tenantId1, filters, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(3);
    }

    @Test
    void findPaymentsWithFilters_WithNullFilters_ShouldReturnAll() {
        Pageable pageable = PageRequest.of(0, 10);
        PaymentFilterDto filters = PaymentFilterDto.builder().build();

        Page<Payment> result = paymentRepository.findPaymentsWithFilters(tenantId1, filters, pageable);

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent()).allMatch(p -> p.getTenantId().equals(tenantId1));
    }

    @Test
    void findPaymentsWithFilters_WithAmountRange_ShouldFilterByAmount() {
        Pageable pageable = PageRequest.of(0, 10);
        PaymentFilterDto filters = PaymentFilterDto.builder()
                .amountMin(12000L)
                .amountMax(18000L)
                .build();

        Page<Payment> result = paymentRepository.findPaymentsWithFilters(tenantId1, filters, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAmount()).isEqualTo(15000L);
    }

    @Test
    void sumRevenueByTenantId_ShouldSumCompletedPayments() {
        Instant dateFrom = Instant.now().minusSeconds(7200);
        Instant dateTo = Instant.now();

        Long revenue = paymentRepository.sumRevenueByTenantId(tenantId1, dateFrom, dateTo);

        assertThat(revenue).isEqualTo(25000L);
    }

    @Test
    void sumRevenueByTenantId_WithDateRange_ShouldFilterByDates() {
        Instant dateFrom = Instant.now().minusSeconds(1800);
        Instant dateTo = Instant.now();

        Long revenue = paymentRepository.sumRevenueByTenantId(tenantId1, dateFrom, dateTo);

        assertThat(revenue).isEqualTo(15000L);
    }

    @Test
    void sumRevenueByTenantId_WithNoCompletedPayments_ShouldReturnZero() {
        Instant dateFrom = Instant.now().plusSeconds(3600);
        Instant dateTo = Instant.now().plusSeconds(7200);

        Long revenue = paymentRepository.sumRevenueByTenantId(tenantId1, dateFrom, dateTo);

        assertThat(revenue).isZero();
    }

    @Test
    void countByTenantId_ShouldCountAllNonDeletedPayments() {
        Instant dateFrom = Instant.now().minusSeconds(7200);
        Instant dateTo = Instant.now();

        Long count = paymentRepository.countByTenantId(tenantId1, dateFrom, dateTo);

        assertThat(count).isEqualTo(3L);
    }

    @Test
    void countByTenantIdAndStatus_ShouldCountByStatus() {
        Instant dateFrom = Instant.now().minusSeconds(7200);
        Instant dateTo = Instant.now();

        Long completedCount = paymentRepository.countByTenantIdAndStatus(tenantId1, PaymentStatus.COMPLETED, dateFrom, dateTo);
        Long pendingCount = paymentRepository.countByTenantIdAndStatus(tenantId1, PaymentStatus.PENDING, dateFrom, dateTo);

        assertThat(completedCount).isEqualTo(2L);
        assertThat(pendingCount).isEqualTo(1L);
    }

    @Test
    void sumRefundedAmountByTenantId_ShouldSumRefundedAmounts() {
        payment1.setRefundedAmount(5000L);
        payment3.setRefundedAmount(3000L);
        paymentRepository.saveAll(List.of(payment1, payment3));

        Instant dateFrom = Instant.now().minusSeconds(7200);
        Instant dateTo = Instant.now();

        Long refunded = paymentRepository.sumRefundedAmountByTenantId(tenantId1, dateFrom, dateTo);

        assertThat(refunded).isEqualTo(8000L);
    }

    @Test
    void avgPaymentAmountByTenantId_ShouldCalculateAverage() {
        Instant dateFrom = Instant.now().minusSeconds(7200);
        Instant dateTo = Instant.now();

        Long average = paymentRepository.avgPaymentAmountByTenantId(tenantId1, dateFrom, dateTo);

        assertThat(average).isGreaterThan(0L);
    }

    @Test
    void findPaymentsWithFilters_ShouldRespectTenantIsolation() {
        Pageable pageable = PageRequest.of(0, 10);
        PaymentFilterDto filters = PaymentFilterDto.builder().build();

        Page<Payment> tenant1Payments = paymentRepository.findPaymentsWithFilters(tenantId1, filters, pageable);
        Page<Payment> tenant2Payments = paymentRepository.findPaymentsWithFilters(tenantId2, filters, pageable);

        assertThat(tenant1Payments.getContent()).hasSize(3);
        assertThat(tenant2Payments.getContent()).hasSize(1);
        assertThat(tenant1Payments.getContent()).noneMatch(p -> p.getTenantId().equals(tenantId2));
        assertThat(tenant2Payments.getContent()).noneMatch(p -> p.getTenantId().equals(tenantId1));
    }

    @Test
    void softDelete_ShouldExcludeDeletedPayments() {
        payment1.setDeletedAt(Instant.now());
        paymentRepository.save(payment1);

        Pageable pageable = PageRequest.of(0, 10);
        PaymentFilterDto filters = PaymentFilterDto.builder().build();
        Page<Payment> result = paymentRepository.findPaymentsWithFilters(tenantId1, filters, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).noneMatch(p -> p.getId().equals(payment1.getId()));
    }

    @Test
    void findByIdempotencyKey_ShouldReturnPayment() {
        String idempotencyKey = "test-key-123";
        if (payment1.getMetadata() == null) {
            payment1.setMetadata(new java.util.HashMap<>());
        }
        payment1.getMetadata().put("idempotencyKey", idempotencyKey);
        paymentRepository.save(payment1);
        paymentRepository.flush();

        Optional<Payment> result = paymentRepository.findByIdempotencyKey(tenantId1, idempotencyKey);

        assertThat(result).isPresent();
        assertThat(result.get().getMetadata())
                .isNotNull()
                .containsEntry("idempotencyKey", idempotencyKey);
    }

    @Test
    void findByIdempotencyKey_WithNonExistentKey_ShouldReturnEmpty() {
        Optional<Payment> result = paymentRepository.findByIdempotencyKey(tenantId1, "non-existent");

        assertThat(result).isEmpty();
    }

    @Test
    void transaction_RollbackOnError() {
        Payment invalidPayment = new Payment();
        invalidPayment.setTenantId(null);
        invalidPayment.setCustomerId(customerId1);
        invalidPayment.setAmount(1000L);

        try {
            paymentRepository.save(invalidPayment);
            paymentRepository.flush();
        } catch (Exception e) {
            Long count = paymentRepository.countByTenantId(tenantId1, null, null);
            assertThat(count).isEqualTo(3L);
        }
    }
}

