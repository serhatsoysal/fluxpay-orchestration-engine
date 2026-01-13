package com.fluxpay.security.session.service;

import com.fluxpay.common.exception.SessionExpiredException;
import com.fluxpay.common.exception.SessionInvalidException;
import com.fluxpay.security.jwt.JwtTokenProvider;
import com.fluxpay.security.session.config.SessionProperties;
import com.fluxpay.security.session.model.SessionData;
import com.fluxpay.security.session.repository.SessionRedisRepository;
import com.fluxpay.security.session.util.SessionTestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SessionServiceTest {

    @Mock
    private SessionRedisRepository sessionRepository;

    @Mock
    private SessionSecurityService securityService;

    @Mock
    private SessionAuditService auditService;

    @Mock
    private SessionProperties sessionProperties;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private SessionService sessionService;

    private SessionData testSession;
    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        testSession = SessionTestDataFactory.createSessionData(tenantId, userId);

        SessionProperties.Ttl ttl = new SessionProperties.Ttl();
        ttl.setAccessToken(Duration.ofHours(1));
        ttl.setRefreshToken(Duration.ofDays(30));

        SessionProperties.Concurrent concurrent = new SessionProperties.Concurrent();
        concurrent.setMaxSessions(5);

        SessionProperties.Security security = new SessionProperties.Security();
        security.setFingerprintVerification(true);

        when(sessionProperties.getTtl()).thenReturn(ttl);
        when(sessionProperties.getConcurrent()).thenReturn(concurrent);
        when(sessionProperties.getSecurity()).thenReturn(security);
        when(auditService.logSessionCreated(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(auditService.logSessionTerminated(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(auditService.logTokenRefreshed(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void createSession_ShouldCreateNewSession() {
        doNothing().when(securityService).validateSessionCreation(testSession);
        when(sessionRepository.countActiveSessions(tenantId, userId)).thenReturn(2L);

        SessionData result = sessionService.createSession(testSession);

        assertThat(result).isNotNull();
        assertThat(result.getExpiresAt()).isAfter(Instant.now());
        verify(sessionRepository).save(testSession);
        verify(auditService).logSessionCreated(testSession);
    }

    @Test
    void createSession_ShouldEnforceConcurrentSessionLimit() {
        doNothing().when(securityService).validateSessionCreation(testSession);
        SessionData oldestSession = SessionTestDataFactory.createSessionData(tenantId, userId);
        oldestSession.setCreatedAt(Instant.now().minusSeconds(3600));

        when(sessionRepository.countActiveSessions(tenantId, userId)).thenReturn(5L);
        when(sessionRepository.findOldestSession(tenantId, userId)).thenReturn(oldestSession);
        when(sessionRepository.findBySessionId(eq(tenantId), eq(userId), any())).thenReturn(oldestSession);

        sessionService.createSession(testSession);

        verify(sessionRepository).delete(tenantId, userId, oldestSession.getSessionId());
    }

    @Test
    void getSession_ShouldReturnValidSession() {
        when(sessionRepository.findBySessionId(tenantId, userId, testSession.getSessionId()))
                .thenReturn(testSession);
        when(securityService.validateSession(testSession)).thenReturn(true);

        SessionData result = sessionService.getSession(tenantId, userId, testSession.getSessionId());

        assertThat(result).isNotNull();
        assertThat(result.getSessionId()).isEqualTo(testSession.getSessionId());
    }

    @Test
    void getSession_ShouldReturnNullForInvalidSession() {
        when(sessionRepository.findBySessionId(tenantId, userId, testSession.getSessionId()))
                .thenReturn(testSession);
        when(securityService.validateSession(testSession)).thenReturn(false);

        SessionData result = sessionService.getSession(tenantId, userId, testSession.getSessionId());

        assertThat(result).isNull();
        verify(sessionRepository).delete(tenantId, userId, testSession.getSessionId());
    }

    @Test
    void getSession_ShouldReturnNullForNonExistentSession() {
        when(sessionRepository.findBySessionId(tenantId, userId, testSession.getSessionId()))
                .thenReturn(null);

        SessionData result = sessionService.getSession(tenantId, userId, testSession.getSessionId());

        assertThat(result).isNull();
    }

    @Test
    void updateLastAccess_ShouldUpdateSessionData() throws Exception {
        ArgumentCaptor<SessionData> captor = ArgumentCaptor.forClass(SessionData.class);
        testSession.setRequestCount(1);

        sessionService.updateLastAccess(testSession).get();

        verify(sessionRepository).update(captor.capture());
        SessionData updated = captor.getValue();
        assertThat(updated.getRequestCount()).isEqualTo(2);
        assertThat(updated.getLastAccess()).isNotNull();
    }

    @Test
    void invalidateSession_ShouldDeleteAndBlacklistTokens() {
        when(sessionRepository.findBySessionId(tenantId, userId, testSession.getSessionId()))
                .thenReturn(testSession);

        sessionService.invalidateSession(tenantId, userId, testSession.getSessionId());

        verify(sessionRepository).delete(tenantId, userId, testSession.getSessionId());
        verify(sessionRepository, atLeastOnce()).blacklistToken(any(), any());
        verify(auditService).logSessionTerminated(testSession, "Manual invalidation");
    }

    @Test
    void invalidateAllUserSessions_ShouldInvalidateAllExceptExcluded() {
        SessionData session1 = SessionTestDataFactory.createSessionData(tenantId, userId);
        SessionData session2 = SessionTestDataFactory.createSessionData(tenantId, userId);
        SessionData session3 = SessionTestDataFactory.createSessionData(tenantId, userId);

        when(sessionRepository.findAllByUser(tenantId, userId))
                .thenReturn(Arrays.asList(session1, session2, session3));
        when(sessionRepository.findBySessionId(eq(tenantId), eq(userId), any()))
                .thenReturn(session1, session2, session3);

        sessionService.invalidateAllUserSessions(tenantId, userId, session2.getSessionId());

        verify(sessionRepository).delete(tenantId, userId, session1.getSessionId());
        verify(sessionRepository, never()).delete(tenantId, userId, session2.getSessionId());
        verify(sessionRepository).delete(tenantId, userId, session3.getSessionId());
    }

    @Test
    void refreshSession_ShouldRotateTokens() {
        String refreshToken = testSession.getRefreshToken();
        String deviceFingerprint = testSession.getDeviceFingerprint();

        when(sessionRepository.findByRefreshToken(refreshToken)).thenReturn(testSession);
        when(securityService.verifyDeviceFingerprint(testSession, deviceFingerprint)).thenReturn(true);
        when(jwtTokenProvider.createToken(userId, tenantId, testSession.getRole(), testSession.getSessionId()))
                .thenReturn("new-access-token");

        SessionData result = sessionService.refreshSession(refreshToken, deviceFingerprint);

        assertThat(result.getAccessToken()).isEqualTo("new-access-token");
        assertThat(result.getRefreshToken()).isNotEqualTo(refreshToken);
        verify(sessionRepository).update(testSession);
        verify(auditService).logTokenRefreshed(testSession);
    }

    @Test
    void refreshSession_ShouldThrowExceptionForExpiredToken() {
        testSession.setRefreshTokenExpiresAt(Instant.now().minusSeconds(3600));
        when(sessionRepository.findByRefreshToken(testSession.getRefreshToken())).thenReturn(testSession);

        assertThatThrownBy(() -> sessionService.refreshSession(testSession.getRefreshToken(), "fingerprint"))
                .isInstanceOf(SessionExpiredException.class)
                .hasMessageContaining("Refresh token expired or invalid");
    }

    @Test
    void refreshSession_ShouldThrowExceptionForInvalidFingerprint() {
        when(sessionRepository.findByRefreshToken(testSession.getRefreshToken())).thenReturn(testSession);
        when(securityService.verifyDeviceFingerprint(testSession, "wrong-fingerprint")).thenReturn(false);

        assertThatThrownBy(() -> sessionService.refreshSession(testSession.getRefreshToken(), "wrong-fingerprint"))
                .isInstanceOf(SessionInvalidException.class)
                .hasMessageContaining("Device verification failed");

        verify(securityService).recordSuspiciousActivity(testSession, "Device fingerprint mismatch");
    }

    @Test
    void getUserSessions_ShouldReturnAllUserSessions() {
        List<SessionData> sessions = Arrays.asList(
                SessionTestDataFactory.createSessionData(tenantId, userId),
                SessionTestDataFactory.createSessionData(tenantId, userId)
        );

        when(sessionRepository.findAllByUser(tenantId, userId)).thenReturn(sessions);

        List<SessionData> result = sessionService.getUserSessions(tenantId, userId);

        assertThat(result).hasSize(2);
        verify(sessionRepository).findAllByUser(tenantId, userId);
    }

    @Test
    void isTokenBlacklisted_ShouldReturnBlacklistStatus() {
        String token = "test-token";
        when(sessionRepository.isTokenBlacklisted(token)).thenReturn(true);

        boolean result = sessionService.isTokenBlacklisted(token);

        assertThat(result).isTrue();
        verify(sessionRepository).isTokenBlacklisted(token);
    }

    @Test
    void invalidateAllUserSessions_ShouldInvalidateAll_WhenExcludeSessionIdIsNull() {
        SessionData session1 = SessionTestDataFactory.createSessionData(tenantId, userId);
        SessionData session2 = SessionTestDataFactory.createSessionData(tenantId, userId);

        when(sessionRepository.findAllByUser(tenantId, userId))
                .thenReturn(Arrays.asList(session1, session2));
        when(sessionRepository.findBySessionId(eq(tenantId), eq(userId), any()))
                .thenReturn(session1, session2);

        sessionService.invalidateAllUserSessions(tenantId, userId, null);

        verify(sessionRepository).delete(tenantId, userId, session1.getSessionId());
        verify(sessionRepository).delete(tenantId, userId, session2.getSessionId());
    }

    @Test
    void refreshSession_ShouldThrowException_WhenSessionNotFound() {
        when(sessionRepository.findByRefreshToken("invalid-token")).thenReturn(null);

        assertThatThrownBy(() -> sessionService.refreshSession("invalid-token", "fingerprint"))
                .isInstanceOf(SessionExpiredException.class)
                .hasMessageContaining("Refresh token expired or invalid");
    }

    @Test
    void refreshSession_ShouldThrowException_WhenRefreshTokenExpiresAtIsNull() {
        testSession.setRefreshTokenExpiresAt(null);
        when(sessionRepository.findByRefreshToken(testSession.getRefreshToken())).thenReturn(testSession);

        assertThatThrownBy(() -> sessionService.refreshSession(testSession.getRefreshToken(), "fingerprint"))
                .isInstanceOf(SessionExpiredException.class)
                .hasMessageContaining("Refresh token expired or invalid");
    }

    @Test
    void invalidateSession_ShouldHandleNullSession() {
        when(sessionRepository.findBySessionId(tenantId, userId, "non-existent"))
                .thenReturn(null);

        sessionService.invalidateSession(tenantId, userId, "non-existent");

        verify(sessionRepository, never()).delete(any(), any(), any());
    }

    @Test
    void invalidateSession_ShouldBlacklistTokens_WhenTokensExist() {
        testSession.setAccessToken("access-token");
        testSession.setExpiresAt(Instant.now().plusSeconds(3600));
        testSession.setRefreshToken("refresh-token");
        testSession.setRefreshTokenExpiresAt(Instant.now().plusSeconds(86400));

        when(sessionRepository.findBySessionId(tenantId, userId, testSession.getSessionId()))
                .thenReturn(testSession);

        sessionService.invalidateSession(tenantId, userId, testSession.getSessionId());

        verify(sessionRepository, times(2)).blacklistToken(anyString(), any(Duration.class));
    }

    @Test
    void invalidateSession_ShouldNotBlacklistExpiredTokens() {
        testSession.setAccessToken("access-token");
        testSession.setExpiresAt(Instant.now().minusSeconds(3600));
        testSession.setRefreshToken("refresh-token");
        testSession.setRefreshTokenExpiresAt(Instant.now().minusSeconds(3600));

        when(sessionRepository.findBySessionId(tenantId, userId, testSession.getSessionId()))
                .thenReturn(testSession);

        sessionService.invalidateSession(tenantId, userId, testSession.getSessionId());

        verify(sessionRepository, never()).blacklistToken(anyString(), any(Duration.class));
    }
}

