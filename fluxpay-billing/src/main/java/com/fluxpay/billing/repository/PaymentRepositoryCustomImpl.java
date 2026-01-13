package com.fluxpay.billing.repository;

import com.fluxpay.billing.dto.PaymentFilterDto;
import com.fluxpay.billing.entity.Payment;
import com.fluxpay.common.enums.PaymentMethod;
import com.fluxpay.common.enums.PaymentStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Repository
public class PaymentRepositoryCustomImpl implements PaymentRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Payment> findPaymentsWithFilters(UUID tenantId, PaymentFilterDto filters, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Payment> query = cb.createQuery(Payment.class);
        Root<Payment> root = query.from(Payment.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("tenantId"), tenantId));
        predicates.add(cb.isNull(root.get("deletedAt")));

        if (filters.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), filters.getStatus()));
        }
        if (filters.getPaymentMethod() != null) {
            predicates.add(cb.equal(root.get("paymentMethod"), filters.getPaymentMethod()));
        }
        if (filters.getInvoiceId() != null) {
            predicates.add(cb.equal(root.get("invoiceId"), filters.getInvoiceId()));
        }
        if (filters.getCustomerId() != null) {
            predicates.add(cb.equal(root.get("customerId"), filters.getCustomerId()));
        }
        if (filters.getDateFrom() != null) {
            Instant dateFromInstant = filters.getDateFrom().atStartOfDay().toInstant(ZoneOffset.UTC);
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFromInstant));
        }
        if (filters.getDateTo() != null) {
            Instant dateToInstant = filters.getDateTo().atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateToInstant));
        }
        if (filters.getAmountMin() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), filters.getAmountMin()));
        }
        if (filters.getAmountMax() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("amount"), filters.getAmountMax()));
        }

        query.where(predicates.toArray(new Predicate[0]));

        TypedQuery<Payment> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Payment> countRoot = countQuery.from(Payment.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(predicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        List<Payment> results = typedQuery.getResultList();
        return new PageImpl<>(results, pageable, total);
    }
}

