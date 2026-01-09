package com.fluxpay.security.jwt;

import com.fluxpay.security.context.TenantContext;
import com.fluxpay.security.session.model.DeviceInfo;
import com.fluxpay.security.session.model.SessionData;
import com.fluxpay.security.session.service.DeviceFingerprintService;
import com.fluxpay.security.session.service.SessionSecurityService;
import com.fluxpay.security.session.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private SessionService sessionService;

    @Mock
    private SessionSecurityService sessionSecurityService;

    @Mock
    private DeviceFingerprintService deviceFingerprintService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UUID userId;
    private UUID tenantId;
    private String role;
    private String token;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        role = "USER";
        token = "valid.jwt.token";
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void testDoFilterInternalWithValidToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(sessionService.isTokenBlacklisted(token)).thenReturn(false);
        when(jwtTokenProvider.getUserId(token)).thenReturn(userId);
        when(jwtTokenProvider.getTenantId(token)).thenReturn(tenantId);
        when(jwtTokenProvider.getRole(token)).thenReturn(role);
        when(jwtTokenProvider.getSessionId(token)).thenReturn("session-123");
        
        SessionData mockSession = SessionData.builder()
                .sessionId("session-123")
                .userId(userId)
                .tenantId(tenantId)
                .build();
        when(sessionService.getSession(userId, tenantId, "session-123")).thenReturn(mockSession);
        when(deviceFingerprintService.generateFingerprint(request)).thenReturn("fingerprint");
        when(deviceFingerprintService.extractDeviceInfo(request)).thenReturn(DeviceInfo.builder().build());
        when(deviceFingerprintService.getClientIpAddress(request)).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("test-agent");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(tenantId, TenantContext.getCurrentTenantId());
    }

    @Test
    void testDoFilterInternalWithNoToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternalWithInvalidToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid.token");
        when(jwtTokenProvider.validateToken("invalid.token")).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternalWithBlacklistedToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(sessionService.isTokenBlacklisted(token)).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(sessionService, never()).getSession(any(), any(), any());
    }

    @Test
    void testDoFilterInternalWithExistingSession() throws ServletException, IOException {
        String sessionId = UUID.randomUUID().toString();
        SessionData existingSession = SessionData.builder()
                .sessionId(sessionId)
                .userId(userId)
                .tenantId(tenantId)
                .role(role)
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(sessionService.isTokenBlacklisted(token)).thenReturn(false);
        when(jwtTokenProvider.getUserId(token)).thenReturn(userId);
        when(jwtTokenProvider.getTenantId(token)).thenReturn(tenantId);
        when(jwtTokenProvider.getRole(token)).thenReturn(role);
        when(jwtTokenProvider.getSessionId(token)).thenReturn(sessionId);
        when(sessionService.getSession(tenantId, userId, sessionId)).thenReturn(existingSession);
        when(deviceFingerprintService.generateFingerprint(request)).thenReturn("fingerprint");
        when(sessionSecurityService.verifyDeviceFingerprint(existingSession, "fingerprint")).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(sessionService).updateLastAccess(existingSession);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternalWithInvalidTokenClaims() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(sessionService.isTokenBlacklisted(token)).thenReturn(false);
        when(jwtTokenProvider.getUserId(token)).thenThrow(new IllegalArgumentException("Invalid token"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testDoFilterInternalWithExceptionDuringProcessing() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(sessionService.isTokenBlacklisted(token)).thenReturn(false);
        when(jwtTokenProvider.getUserId(token)).thenReturn(userId);
        when(jwtTokenProvider.getTenantId(token)).thenReturn(tenantId);
        when(jwtTokenProvider.getRole(token)).thenReturn(role);
        when(jwtTokenProvider.getSessionId(token)).thenThrow(new IllegalArgumentException("Invalid session ID"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternalClearsTenantContext() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(TenantContext.getCurrentTenantId());
    }

    @Test
    void testDoFilterInternalWithBearerPrefix() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtTokenProvider).validateToken(token);
    }

    @Test
    void testDoFilterInternalWithDeviceFingerprintMismatch() throws ServletException, IOException {
        String sessionId = UUID.randomUUID().toString();
        SessionData existingSession = SessionData.builder()
                .sessionId(sessionId)
                .userId(userId)
                .tenantId(tenantId)
                .role(role)
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(sessionService.isTokenBlacklisted(token)).thenReturn(false);
        when(jwtTokenProvider.getUserId(token)).thenReturn(userId);
        when(jwtTokenProvider.getTenantId(token)).thenReturn(tenantId);
        when(jwtTokenProvider.getRole(token)).thenReturn(role);
        when(jwtTokenProvider.getSessionId(token)).thenReturn(sessionId);
        when(sessionService.getSession(tenantId, userId, sessionId)).thenReturn(existingSession);
        when(deviceFingerprintService.generateFingerprint(request)).thenReturn("different-fingerprint");
        when(sessionSecurityService.verifyDeviceFingerprint(existingSession, "different-fingerprint")).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(sessionSecurityService).recordSuspiciousActivity(existingSession, "Device fingerprint mismatch");
        verify(filterChain).doFilter(request, response);
    }
}

