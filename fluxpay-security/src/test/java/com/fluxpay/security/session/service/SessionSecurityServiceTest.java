package com.fluxpay.security.session.service;

import com.fluxpay.common.exception.RateLimitExceededException;
import com.fluxpay.security.session.config.SessionProperties;
import com.fluxpay.security.session.model.SessionData;
import com.fluxpay.security.session.repository.SessionRedisRepository;
import com.fluxpay.security.session.util.SessionTestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SessionSecurityServiceTest {

    @Mock
    private SessionRedisRepository sessionRepository;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private SessionProperties sessionProperties;

    @Mock
    private SessionAuditService auditService;

    @InjectMocks
    private SessionSecurityService sessionSecurityService;

    private SessionData testSession;

    @BeforeEach
    void setUp() {
        testSession = SessionTestDataFactory.createSessionData();
        
        SessionProperties.Security security = new SessionProperties.Security();
        security.setFingerprintVerification(true);
        when(sessionProperties.getSecurity()).thenReturn(security);
    }

    @Test
    void validateSessionCreation_ShouldPass_WhenNotRateLimited() {
        when(rateLimitService.isRateLimited(testSession.getIpAddress(), "session_creation")).thenReturn(false);

        sessionSecurityService.validateSessionCreation(testSession);

        verify(rateLimitService).isRateLimited(testSession.getIpAddress(), "session_creation");
    }

    @Test
    void validateSessionCreation_ShouldThrowException_WhenRateLimited() {
        when(rateLimitService.isRateLimited(testSession.getIpAddress(), "session_creation")).thenReturn(true);

        try {
            sessionSecurityService.validateSessionCreation(testSession);
        } catch (RateLimitExceededException e) {
            assertThat(e.getMessage()).contains("Too many session creation attempts");
        }
    }

    @Test
    void validateSession_ShouldReturnTrue_ForValidSession() {
        when(sessionRepository.isTokenBlacklisted(testSession.getAccessToken())).thenReturn(false);
        when(rateLimitService.isRateLimited(testSession.getSessionId(), "session_requests")).thenReturn(false);

        boolean result = sessionSecurityService.validateSession(testSession);

        assertThat(result).isTrue();
    }

    @Test
    void validateSession_ShouldReturnFalse_WhenExpired() {
        testSession.setExpiresAt(Instant.now().minusSeconds(3600));

        boolean result = sessionSecurityService.validateSession(testSession);

        assertThat(result).isFalse();
    }

    @Test
    void validateSession_ShouldReturnFalse_WhenBlacklisted() {
        when(sessionRepository.isTokenBlacklisted(testSession.getAccessToken())).thenReturn(true);

        boolean result = sessionSecurityService.validateSession(testSession);

        assertThat(result).isFalse();
    }

    @Test
    void validateSession_ShouldReturnFalse_WhenSuspiciousActivity() {
        testSession.getSecurityFlags().setSuspiciousActivity(true);

        boolean result = sessionSecurityService.validateSession(testSession);

        assertThat(result).isFalse();
    }

    @Test
    void validateSession_ShouldReturnFalse_WhenRateLimited() {
        when(sessionRepository.isTokenBlacklisted(testSession.getAccessToken())).thenReturn(false);
        when(rateLimitService.isRateLimited(testSession.getSessionId(), "session_requests")).thenReturn(true);

        boolean result = sessionSecurityService.validateSession(testSession);

        assertThat(result).isFalse();
        verify(sessionRepository).update(testSession);
    }

    @Test
    void recordSuspiciousActivity_ShouldUpdateFlags() {
        sessionSecurityService.recordSuspiciousActivity(testSession, "test reason");

        assertThat(testSession.getSecurityFlags().isSuspiciousActivity()).isTrue();
        assertThat(testSession.getSecurityFlags().getFailedAttempts()).isEqualTo(1);
        verify(sessionRepository).update(testSession);
        verify(auditService).logSecurityEvent(eq(testSession), eq("SUSPICIOUS_ACTIVITY"), eq("test reason"));
    }

    @Test
    void verifyDeviceFingerprint_ShouldReturnTrue_WhenMatches() {
        boolean result = sessionSecurityService.verifyDeviceFingerprint(testSession, testSession.getDeviceFingerprint());

        assertThat(result).isTrue();
    }

    @Test
    void verifyDeviceFingerprint_ShouldReturnFalse_WhenNotMatches() {
        boolean result = sessionSecurityService.verifyDeviceFingerprint(testSession, "different-fingerprint");

        assertThat(result).isFalse();
    }

    @Test
    void verifyDeviceFingerprint_ShouldReturnTrue_WhenVerificationDisabled() {
        SessionProperties.Security security = new SessionProperties.Security();
        security.setFingerprintVerification(false);
        when(sessionProperties.getSecurity()).thenReturn(security);

        boolean result = sessionSecurityService.verifyDeviceFingerprint(testSession, "any-fingerprint");

        assertThat(result).isTrue();
    }
}


