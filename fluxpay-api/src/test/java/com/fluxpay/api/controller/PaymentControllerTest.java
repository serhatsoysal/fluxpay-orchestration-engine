package com.fluxpay.api.controller;

import com.fluxpay.api.dto.CreateRefundRequest;
import com.fluxpay.api.dto.RefundResponse;
import com.fluxpay.billing.entity.Payment;
import com.fluxpay.billing.entity.Refund;
import com.fluxpay.billing.service.PaymentService;
import com.fluxpay.common.dto.PageResponse;
import com.fluxpay.common.dto.PaymentStatsResponse;
import com.fluxpay.common.dto.Period;
import com.fluxpay.common.enums.PaymentMethod;
import com.fluxpay.common.enums.PaymentStatus;
import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.common.exception.ValidationException;
import com.fluxpay.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private PaymentController paymentController;

    private Payment payment;
    private Refund refund;
    private UUID paymentId;
    private UUID tenantId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        payment = new Payment();
        payment.setId(paymentId);
        payment.setTenantId(tenantId);
        payment.setCustomerId(customerId);
        payment.setAmount(10000L);
        payment.setCurrency("USD");
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        payment.setCreatedAt(Instant.now());

        refund = new Refund();
        refund.setId(UUID.randomUUID());
        refund.setPaymentId(paymentId);
        refund.setAmount(5000L);
        refund.setCurrency("USD");
        refund.setStatus(PaymentStatus.COMPLETED);
        refund.setReason("Customer requested");
        refund.setRefundId("re_12345");
        refund.setCreatedAt(Instant.now());
    }

    @Test
    void getPayments_ShouldReturnOkWithPageResponse() {
        PageResponse<Payment> pageResponse = new PageResponse<>(
                List.of(payment), 0, 20, 1L, 1, true
        );

        when(paymentService.getPayments(
                anyInt(), anyInt(), any(com.fluxpay.billing.dto.PaymentFilterDto.class)
        )).thenReturn(pageResponse);

        ResponseEntity<PageResponse<Payment>> response = paymentController.getPayments(
                0, 20, null, null, null, null, null, null, null, null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        verify(paymentService).getPayments(
                eq(0), eq(20), any(com.fluxpay.billing.dto.PaymentFilterDto.class)
        );
    }

    @Test
    void getPayments_WithFilters_ShouldCallServiceWithFilters() {
        PageResponse<Payment> pageResponse = new PageResponse<>(
                List.of(payment), 0, 20, 1L, 1, true
        );
        LocalDate dateFrom = LocalDate.now().minusDays(30);
        LocalDate dateTo = LocalDate.now();

        when(paymentService.getPayments(
                anyInt(), anyInt(), any(com.fluxpay.billing.dto.PaymentFilterDto.class)
        )).thenReturn(pageResponse);

        ResponseEntity<PageResponse<Payment>> response = paymentController.getPayments(
                0, 20, PaymentStatus.COMPLETED, PaymentMethod.CREDIT_CARD, UUID.randomUUID(),
                customerId, dateFrom, dateTo, 1000L, 50000L
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paymentService).getPayments(
                eq(0), eq(20), any(com.fluxpay.billing.dto.PaymentFilterDto.class)
        );
    }

    @Test
    void getPayment_ShouldReturnOkWithPayment() {
        when(paymentService.getPaymentById(paymentId)).thenReturn(payment);

        ResponseEntity<Payment> response = paymentController.getPayment(paymentId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(paymentId);
        verify(paymentService).getPaymentById(paymentId);
    }

    @Test
    void getPayment_NotFound_ShouldThrowException() {
        when(paymentService.getPaymentById(paymentId))
                .thenThrow(new ResourceNotFoundException("Payment", paymentId));

        try {
            paymentController.getPayment(paymentId);
        } catch (ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("Payment");
        }

        verify(paymentService).getPaymentById(paymentId);
    }

    @Test
    void getPaymentStats_ShouldReturnOkWithStats() {
        PaymentStatsResponse stats = new PaymentStatsResponse(
                100000L, 10L, 8L, 1L, 1L, 5000L, 10000L, "USD",
                new Period(LocalDate.now().minusDays(30), LocalDate.now())
        );

        when(paymentService.getPaymentStats(any(), any())).thenReturn(stats);

        ResponseEntity<PaymentStatsResponse> response = paymentController.getPaymentStats(null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTotalRevenue()).isEqualTo(100000L);
        verify(paymentService).getPaymentStats(null, null);
    }

    @Test
    void getPaymentStats_WithDateRange_ShouldCallServiceWithDates() {
        LocalDate dateFrom = LocalDate.now().minusDays(30);
        LocalDate dateTo = LocalDate.now();
        PaymentStatsResponse stats = new PaymentStatsResponse(
                50000L, 5L, 4L, 1L, 0L, 2000L, 10000L, "USD",
                new Period(dateFrom, dateTo)
        );

        when(paymentService.getPaymentStats(dateFrom, dateTo)).thenReturn(stats);

        ResponseEntity<PaymentStatsResponse> response = paymentController.getPaymentStats(dateFrom, dateTo);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paymentService).getPaymentStats(dateFrom, dateTo);
    }

    @Test
    void createRefund_WithOwnerRole_ShouldReturnOk() {
        CreateRefundRequest request = new CreateRefundRequest();
        request.setAmount(5000L);
        request.setReason("Customer requested");
        request.setMetadata(Map.of("source", "dashboard"));

        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtTokenProvider.getRole("valid-token")).thenReturn("OWNER");
        when(paymentService.createRefund(eq(paymentId), eq(5000L), eq("Customer requested"), any()))
                .thenReturn(refund);

        ResponseEntity<RefundResponse> response = paymentController.createRefund(
                paymentId, request, httpRequest
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getPaymentId()).isEqualTo(paymentId);
        assertThat(response.getBody().getAmount()).isEqualTo(5000L);
        verify(paymentService).createRefund(eq(paymentId), eq(5000L), eq("Customer requested"), any());
    }

    @Test
    void createRefund_WithAdminRole_ShouldReturnOk() {
        CreateRefundRequest request = new CreateRefundRequest();
        request.setAmount(5000L);
        request.setReason("Customer requested");

        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtTokenProvider.getRole("valid-token")).thenReturn("ADMIN");
        when(paymentService.createRefund(eq(paymentId), eq(5000L), eq("Customer requested"), any()))
                .thenReturn(refund);

        ResponseEntity<RefundResponse> response = paymentController.createRefund(
                paymentId, request, httpRequest
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paymentService).createRefund(eq(paymentId), eq(5000L), eq("Customer requested"), any());
    }

    @Test
    void createRefund_WithUserRole_ShouldReturnForbidden() {
        CreateRefundRequest request = new CreateRefundRequest();
        request.setAmount(5000L);

        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtTokenProvider.getRole("valid-token")).thenReturn("USER");

        ResponseEntity<RefundResponse> response = paymentController.createRefund(
                paymentId, request, httpRequest
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(paymentService, never()).createRefund(any(), any(), any(), any());
    }

    @Test
    void createRefund_WithoutAuthorizationHeader_ShouldReturnUnauthorized() {
        CreateRefundRequest request = new CreateRefundRequest();
        request.setAmount(5000L);

        when(httpRequest.getHeader("Authorization")).thenReturn(null);

        ResponseEntity<RefundResponse> response = paymentController.createRefund(
                paymentId, request, httpRequest
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(paymentService, never()).createRefund(any(), any(), any(), any());
    }

    @Test
    void createRefund_WithInvalidToken_ShouldReturnUnauthorized() {
        CreateRefundRequest request = new CreateRefundRequest();
        request.setAmount(5000L);

        when(httpRequest.getHeader("Authorization")).thenReturn("Invalid token");

        ResponseEntity<RefundResponse> response = paymentController.createRefund(
                paymentId, request, httpRequest
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(paymentService, never()).createRefund(any(), any(), any(), any());
        verify(jwtTokenProvider, never()).getRole(any());
    }

    @Test
    void createRefund_WhenPaymentNotFound_ShouldThrowException() {
        CreateRefundRequest request = new CreateRefundRequest();
        request.setAmount(5000L);

        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtTokenProvider.getRole("valid-token")).thenReturn("OWNER");
        when(paymentService.createRefund(eq(paymentId), eq(5000L), any(), any()))
                .thenThrow(new ResourceNotFoundException("Payment", paymentId));

        try {
            paymentController.createRefund(paymentId, request, httpRequest);
        } catch (ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("Payment");
        }

        verify(paymentService).createRefund(eq(paymentId), eq(5000L), any(), any());
    }

    @Test
    void createRefund_WhenValidationFails_ShouldThrowException() {
        CreateRefundRequest request = new CreateRefundRequest();
        request.setAmount(5000L);

        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtTokenProvider.getRole("valid-token")).thenReturn("OWNER");
        when(paymentService.createRefund(eq(paymentId), eq(5000L), any(), any()))
                .thenThrow(new ValidationException("Refund amount exceeds refundable amount"));

        try {
            paymentController.createRefund(paymentId, request, httpRequest);
        } catch (ValidationException e) {
            assertThat(e.getMessage()).contains("Refund amount");
        }

        verify(paymentService).createRefund(eq(paymentId), eq(5000L), any(), any());
    }

    @Test
    void extractToken_WithBearerToken_ShouldReturnToken() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer test-token");

        var method = PaymentController.class.getDeclaredMethod("extractToken", HttpServletRequest.class);
        method.setAccessible(true);
        String token = (String) method.invoke(paymentController, request);

        assertThat(token).isEqualTo("test-token");
    }

    @Test
    void extractToken_WithoutBearerPrefix_ShouldReturnNull() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Invalid token");

        var method = PaymentController.class.getDeclaredMethod("extractToken", HttpServletRequest.class);
        method.setAccessible(true);
        String token = (String) method.invoke(paymentController, request);

        assertThat(token).isNull();
    }

    @Test
    void extractToken_WithNullHeader_ShouldReturnNull() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(null);

        var method = PaymentController.class.getDeclaredMethod("extractToken", HttpServletRequest.class);
        method.setAccessible(true);
        String token = (String) method.invoke(paymentController, request);

        assertThat(token).isNull();
    }
}

