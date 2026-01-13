package com.fluxpay.billing.repository;

import com.fluxpay.billing.entity.Invoice;
import com.fluxpay.common.enums.InvoiceStatus;
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
class InvoiceRepositoryIT {

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
    private InvoiceRepository invoiceRepository;

    private UUID tenantId1;
    private UUID tenantId2;
    private UUID customerId1;
    private UUID customerId2;
    private UUID subscriptionId1;
    private Invoice invoice1;
    private Invoice invoice2;
    private Invoice invoice3;
    private Invoice invoice4;

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();

        tenantId1 = UUID.randomUUID();
        tenantId2 = UUID.randomUUID();
        customerId1 = UUID.randomUUID();
        customerId2 = UUID.randomUUID();
        subscriptionId1 = UUID.randomUUID();

        LocalDate today = LocalDate.now();
        LocalDate pastDueDate = today.minusDays(5);
        LocalDate futureDueDate = today.plusDays(14);

        invoice1 = createInvoice(tenantId1, customerId1, subscriptionId1, InvoiceStatus.OPEN, 10000L, 10000L, 0L, pastDueDate, "INV-001");
        invoice2 = createInvoice(tenantId1, customerId1, subscriptionId1, InvoiceStatus.OPEN, 20000L, 15000L, 5000L, futureDueDate, "INV-002");
        invoice3 = createInvoice(tenantId1, customerId2, null, InvoiceStatus.PAID, 15000L, 0L, 15000L, pastDueDate, "INV-003");
        invoice4 = createInvoice(tenantId2, customerId1, null, InvoiceStatus.DRAFT, 5000L, 5000L, 0L, futureDueDate, "INV-004");

        invoiceRepository.saveAll(List.of(invoice1, invoice2, invoice3, invoice4));
    }

    private Invoice createInvoice(UUID tenantId, UUID customerId, UUID subscriptionId, InvoiceStatus status, Long total, Long amountDue, Long amountPaid, LocalDate dueDate, String invoiceNumber) {
        Invoice invoice = new Invoice();
        invoice.setTenantId(tenantId);
        invoice.setCustomerId(customerId);
        invoice.setSubscriptionId(subscriptionId);
        invoice.setStatus(status);
        invoice.setTotal(total);
        invoice.setSubtotal(total);
        invoice.setAmountDue(amountDue);
        invoice.setAmountPaid(amountPaid);
        invoice.setDueDate(dueDate);
        invoice.setInvoiceDate(dueDate.minusDays(30));
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setCurrency("USD");
        invoice.setCreatedAt(Instant.now());
        return invoice;
    }

    @Test
    void findByTenantIdAndCustomerId_ShouldReturnOnlyTenantInvoices() {
        List<Invoice> invoices = invoiceRepository.findByTenantIdAndCustomerId(tenantId1, customerId1);

        assertThat(invoices).hasSize(2);
        assertThat(invoices).allMatch(i -> i.getTenantId().equals(tenantId1));
        assertThat(invoices).allMatch(i -> i.getCustomerId().equals(customerId1));
    }

    @Test
    void findBySubscriptionId_ShouldReturnAllInvoicesForSubscription() {
        List<Invoice> invoices = invoiceRepository.findBySubscriptionId(subscriptionId1);

        assertThat(invoices).hasSize(2);
        assertThat(invoices).allMatch(i -> i.getSubscriptionId().equals(subscriptionId1));
    }

    @Test
    void findByStatusAndDueDateBefore_ShouldReturnOverdueInvoices() {
        LocalDate today = LocalDate.now();
        List<Invoice> overdue = invoiceRepository.findByStatusAndDueDateBefore(InvoiceStatus.OPEN, today);

        assertThat(overdue).hasSize(1);
        assertThat(overdue.get(0).getDueDate()).isBefore(today);
        assertThat(overdue.get(0).getStatus()).isEqualTo(InvoiceStatus.OPEN);
    }

    @Test
    void findByTenantIdAndStatus_WithPagination_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 2);

        Page<Invoice> result = invoiceRepository.findByTenantIdAndStatus(tenantId1, InvoiceStatus.OPEN, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }

    @Test
    void findByTenantIdAndStatus_WithNullStatus_ShouldReturnAll() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<Invoice> result = invoiceRepository.findByTenantIdAndStatus(tenantId1, null, pageable);

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent()).allMatch(i -> i.getTenantId().equals(tenantId1));
    }

    @Test
    void countByTenantId_ShouldCountAllNonDeleted() {
        Long count = invoiceRepository.countByTenantId(tenantId1);

        assertThat(count).isEqualTo(3L);
    }

    @Test
    void countByTenantIdAndStatus_ShouldCountByStatus() {
        Long openCount = invoiceRepository.countByTenantIdAndStatus(tenantId1, InvoiceStatus.OPEN);
        Long paidCount = invoiceRepository.countByTenantIdAndStatus(tenantId1, InvoiceStatus.PAID);
        Long draftCount = invoiceRepository.countByTenantIdAndStatus(tenantId1, InvoiceStatus.DRAFT);

        assertThat(openCount).isEqualTo(2L);
        assertThat(paidCount).isEqualTo(1L);
        assertThat(draftCount).isEqualTo(0L);
    }

    @Test
    void sumTotalByTenantId_ShouldSumAllTotals() {
        Long total = invoiceRepository.sumTotalByTenantId(tenantId1);

        assertThat(total).isEqualTo(45000L);
    }

    @Test
    void sumAmountDueByTenantId_ShouldSumOutstanding() {
        Long amountDue = invoiceRepository.sumAmountDueByTenantId(tenantId1);

        assertThat(amountDue).isEqualTo(25000L);
    }

    @Test
    void sumAmountPaidByTenantId_ShouldSumPaid() {
        Long amountPaid = invoiceRepository.sumAmountPaidByTenantId(tenantId1);

        assertThat(amountPaid).isEqualTo(15000L);
    }

    @Test
    void countOverdueByTenantId_ShouldCountOverdue() {
        LocalDate today = LocalDate.now();

        Long overdueCount = invoiceRepository.countOverdueByTenantId(tenantId1, today);

        assertThat(overdueCount).isEqualTo(1L);
    }

    @Test
    void sumOverdueAmountByTenantId_ShouldSumOverdueAmounts() {
        LocalDate today = LocalDate.now();

        Long overdueAmount = invoiceRepository.sumOverdueAmountByTenantId(tenantId1, today);

        assertThat(overdueAmount).isEqualTo(10000L);
    }

    @Test
    void findTopByTenantIdOrderByCreatedAtDesc_ShouldReturnLatest() {
        Optional<Invoice> latest = invoiceRepository.findTopByTenantIdOrderByCreatedAtDesc(tenantId1);

        assertThat(latest).isPresent();
        assertThat(latest.get().getTenantId()).isEqualTo(tenantId1);
    }

    @Test
    void findByIdempotencyKey_ShouldReturnInvoice() {
        String idempotencyKey = "test-key-123";
        invoice1.setIdempotencyKey(idempotencyKey);
        invoiceRepository.save(invoice1);

        Optional<Invoice> result = invoiceRepository.findByIdempotencyKey(idempotencyKey);

        assertThat(result).isPresent();
        assertThat(result.get().getIdempotencyKey()).isEqualTo(idempotencyKey);
    }

    @Test
    void findByTenantIdAndStatus_ShouldRespectTenantIsolation() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<Invoice> tenant1Invoices = invoiceRepository.findByTenantIdAndStatus(tenantId1, null, pageable);
        Page<Invoice> tenant2Invoices = invoiceRepository.findByTenantIdAndStatus(tenantId2, null, pageable);

        assertThat(tenant1Invoices.getContent()).hasSize(3);
        assertThat(tenant2Invoices.getContent()).hasSize(1);
        assertThat(tenant1Invoices.getContent()).noneMatch(i -> i.getTenantId().equals(tenantId2));
        assertThat(tenant2Invoices.getContent()).noneMatch(i -> i.getTenantId().equals(tenantId1));
    }

    @Test
    void softDelete_ShouldExcludeDeletedInvoices() {
        invoice1.setDeletedAt(Instant.now());
        invoiceRepository.save(invoice1);

        Long count = invoiceRepository.countByTenantId(tenantId1);

        assertThat(count).isEqualTo(2L);
    }

    @Test
    void sumOverdueAmountByTenantId_WithNoOverdue_ShouldReturnZero() {
        invoice1.setStatus(InvoiceStatus.PAID);
        invoiceRepository.save(invoice1);

        LocalDate today = LocalDate.now();
        Long overdueAmount = invoiceRepository.sumOverdueAmountByTenantId(tenantId1, today);

        assertThat(overdueAmount).isEqualTo(0L);
    }

    @Test
    void pagination_WithSorting_ShouldRespectOrder() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("dueDate").descending());

        Page<Invoice> result = invoiceRepository.findByTenantIdAndStatus(tenantId1, null, pageable);

        List<Invoice> content = result.getContent();
        assertThat(content).isNotEmpty();
        for (int i = 0; i < content.size() - 1; i++) {
            assertThat(content.get(i).getDueDate())
                    .isAfterOrEqualTo(content.get(i + 1).getDueDate());
        }
    }
}

