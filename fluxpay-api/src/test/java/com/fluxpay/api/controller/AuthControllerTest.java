package com.fluxpay.api.controller;

import com.fluxpay.api.dto.LoginRequest;
import com.fluxpay.api.dto.LoginResponse;
import com.fluxpay.api.dto.RefreshTokenRequest;
import com.fluxpay.api.dto.RefreshTokenResponse;
import com.fluxpay.api.dto.SessionInfoResponse;
import com.fluxpay.common.exception.ValidationException;
import com.fluxpay.security.context.TenantContext;
import com.fluxpay.security.jwt.JwtTokenProvider;
import com.fluxpay.security.session.config.SessionProperties;
import com.fluxpay.security.session.model.DeviceInfo;
import com.fluxpay.security.session.model.SecurityFlags;
import com.fluxpay.security.session.model.SessionData;
import com.fluxpay.security.session.service.DeviceFingerprintService;
import com.fluxpay.security.session.service.SessionService;
import com.fluxpay.tenant.entity.User;
import com.fluxpay.tenant.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private SessionService sessionService;

    @Mock
    private DeviceFingerprintService deviceFingerprintService;

    @Mock
    private SessionProperties sessionProperties;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthController authController;

    private User user;
    private UUID userId;
    private UUID tenantId;
    private SessionProperties.Ttl ttl;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tenantId = UUID.randomUUID();

        user = new User();
        user.setId(userId);
        user.setTenantId(tenantId);
        user.setEmail("test@example.com");
        user.setRole(com.fluxpay.common.enums.UserRole.ADMIN);

        ttl = new SessionProperties.Ttl();
        ttl.setAccessToken(Duration.ofHours(1));
        ttl.setRefreshToken(Duration.ofDays(30));
        when(sessionProperties.getTtl()).thenReturn(ttl);

        SecurityContextHolder.setContext(securityContext);
        TenantContext.setCurrentTenant(tenantId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        DeviceInfo deviceInfo = DeviceInfo.builder().build();
        String deviceFingerprint = "fingerprint-123";
        String ipAddress = "127.0.0.1";
        String accessToken = "access-token";
        String sessionId = UUID.randomUUID().toString();
        String refreshToken = UUID.randomUUID().toString() + "-" + System.currentTimeMillis();

        when(userService.getUserByEmail("test@example.com")).thenReturn(user);
        when(userService.verifyPassword(user, "password123")).thenReturn(true);
        when(jwtTokenProvider.createToken(eq(userId), eq(tenantId), eq("ADMIN"), anyString()))
                .thenReturn(accessToken);
        when(deviceFingerprintService.extractDeviceInfo(httpRequest)).thenReturn(deviceInfo);
        when(deviceFingerprintService.generateFingerprint(httpRequest)).thenReturn(deviceFingerprint);
        when(deviceFingerprintService.getClientIpAddress(httpRequest)).thenReturn(ipAddress);
        when(httpRequest.getHeader("User-Agent")).thenReturn("test-agent");
        doNothing().when(sessionService).createSession(any(SessionData.class));

        ResponseEntity<LoginResponse> response = authController.login(request, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isEqualTo(accessToken);
        assertThat(response.getBody().getRefreshToken()).isNotNull();
        assertThat(response.getBody().getSessionId()).isNotNull();
        assertThat(response.getBody().getUserId()).isEqualTo(userId.toString());
        assertThat(response.getBody().getTenantId()).isEqualTo(tenantId.toString());
        assertThat(response.getBody().getRole()).isEqualTo("ADMIN");
        verify(sessionService).createSession(any(SessionData.class));
    }

    @Test
    void login_ThrowsException_WhenPasswordInvalid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrong-password");

        when(userService.getUserByEmail("test@example.com")).thenReturn(user);
        when(userService.verifyPassword(user, "wrong-password")).thenReturn(false);

        assertThatThrownBy(() -> authController.login(request, httpRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid credentials");

        verify(sessionService, never()).createSession(any());
    }

    @Test
    void logout_Success_WithSessionId() {
        String sessionId = "session-123";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userId);
        when(authentication.getCredentials()).thenReturn(createSessionData(sessionId));

        doNothing().when(sessionService).invalidateSession(eq(tenantId), eq(userId), eq(sessionId));

        ResponseEntity<Void> response = authController.logout();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(sessionService).invalidateSession(tenantId, userId, sessionId);
    }

    @Test
    void logout_Success_WithoutSessionId() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userId);
        when(authentication.getCredentials()).thenReturn("not-session-data");

        ResponseEntity<Void> response = authController.logout();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(sessionService, never()).invalidateSession(any(), any(), any());
    }

    @Test
    void logoutAll_Success() {
        String currentSessionId = "current-session-123";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userId);
        when(authentication.getCredentials()).thenReturn(createSessionData(currentSessionId));

        doNothing().when(sessionService).invalidateAllUserSessions(eq(tenantId), eq(userId), eq(currentSessionId));

        ResponseEntity<Void> response = authController.logoutAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(sessionService).invalidateAllUserSessions(tenantId, userId, currentSessionId);
    }

    @Test
    void refresh_Success() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token-123");

        String deviceFingerprint = "fingerprint-123";
        SessionData refreshedSession = createSessionData("session-123");
        refreshedSession.setAccessToken("new-access-token");
        refreshedSession.setRefreshToken("new-refresh-token");

        when(deviceFingerprintService.generateFingerprint(httpRequest)).thenReturn(deviceFingerprint);
        when(sessionService.refreshSession("refresh-token-123", deviceFingerprint)).thenReturn(refreshedSession);

        ResponseEntity<RefreshTokenResponse> response = authController.refresh(request, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isEqualTo("new-access-token");
        assertThat(response.getBody().getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.getBody().getSessionId()).isEqualTo("session-123");
    }

    @Test
    void getSessions_Success() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userId);

        SessionData session1 = createSessionData("session-1");
        SessionData session2 = createSessionData("session-2");
        List<SessionData> sessions = List.of(session1, session2);

        when(sessionService.getUserSessions(tenantId, userId)).thenReturn(sessions);

        ResponseEntity<List<SessionInfoResponse>> response = authController.getSessions();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getSessionId()).isEqualTo("session-1");
        assertThat(response.getBody().get(1).getSessionId()).isEqualTo("session-2");
    }

    @Test
    void getSessions_ReturnsEmptyList() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userId);

        when(sessionService.getUserSessions(tenantId, userId)).thenReturn(List.of());

        ResponseEntity<List<SessionInfoResponse>> response = authController.getSessions();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();
    }

    private SessionData createSessionData(String sessionId) {
        return SessionData.builder()
                .sessionId(sessionId)
                .userId(userId)
                .tenantId(tenantId)
                .role("ADMIN")
                .deviceInfo(DeviceInfo.builder().build())
                .ipAddress("127.0.0.1")
                .securityFlags(SecurityFlags.builder().build())
                .createdAt(Instant.now())
                .lastAccess(Instant.now())
                .build();
    }
}

