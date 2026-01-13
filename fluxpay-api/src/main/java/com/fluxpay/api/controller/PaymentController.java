package com.fluxpay.api.controller;

import com.fluxpay.api.dto.CreateRefundRequest;
import com.fluxpay.api.dto.RefundResponse;
import com.fluxpay.common.dto.PaymentStatsResponse;
import com.fluxpay.billing.entity.Payment;
import com.fluxpay.billing.entity.Refund;
import com.fluxpay.billing.service.PaymentService;
import com.fluxpay.common.dto.PageResponse;
import com.fluxpay.common.enums.PaymentMethod;
import com.fluxpay.common.enums.PaymentStatus;
import com.fluxpay.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final JwtTokenProvider jwtTokenProvider;

    public PaymentController(PaymentService paymentService, JwtTokenProvider jwtTokenProvider) {
        this.paymentService = paymentService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping
    public ResponseEntity<PageResponse<Payment>> getPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) UUID invoiceId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Long amountMin,
            @RequestParam(required = false) Long amountMax) {
        PageResponse<Payment> response = paymentService.getPayments(
                page, size, status, paymentMethod, invoiceId, customerId,
                dateFrom, dateTo, amountMin, amountMax);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPayment(@PathVariable UUID id) {
        Payment payment = paymentService.getPaymentById(id);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/stats")
    public ResponseEntity<PaymentStatsResponse> getPaymentStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        PaymentStatsResponse stats = paymentService.getPaymentStats(dateFrom, dateTo);
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<RefundResponse> createRefund(
            @PathVariable UUID id,
            @Valid @RequestBody CreateRefundRequest request,
            HttpServletRequest httpRequest) {
        String token = extractToken(httpRequest);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String role = jwtTokenProvider.getRole(token);
        if (role == null || (!"OWNER".equals(role) && !"ADMIN".equals(role))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        Refund refund = paymentService.createRefund(
                id, request.getAmount(), request.getReason(), request.getMetadata());
        
        RefundResponse response = new RefundResponse(
                refund.getId(),
                refund.getPaymentId(),
                refund.getAmount(),
                refund.getCurrency(),
                refund.getStatus(),
                refund.getReason(),
                refund.getRefundId(),
                refund.getCreatedAt()
        );
        
        return ResponseEntity.ok(response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

