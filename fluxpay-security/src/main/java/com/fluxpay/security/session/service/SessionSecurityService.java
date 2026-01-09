package com.fluxpay.security.session.service;

import com.fluxpay.common.exception.RateLimitExceededException;
import com.fluxpay.security.session.config.SessionProperties;
import com.fluxpay.security.session.model.SessionData;
import com.fluxpay.security.session.repository.SessionRedisRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SessionSecurityService {

    private final SessionRedisRepository sessionRepository;
    private final RateLimitService rateLimitService;
    private final SessionProperties sessionProperties;
    private final SessionAuditService auditService;

    public SessionSecurityService(
            SessionRedisRepository sessionRepository,
            RateLimitService rateLimitService,
            SessionProperties sessionProperties,
            SessionAuditService auditService) {
        this.sessionRepository = sessionRepository;
        this.rateLimitService = rateLimitService;
        this.sessionProperties = sessionProperties;
        this.auditService = auditService;
    }

    public void validateSessionCreation(SessionData session) {
        if (rateLimitService.isRateLimited(session.getIpAddress(), "session_creation")) {
            throw new RateLimitExceededException("Too many session creation attempts");
        }
    }

    public boolean validateSession(SessionData session) {
        if (session.getExpiresAt().isBefore(Instant.now())) {
            return false;
        }
        
        if (sessionRepository.isTokenBlacklisted(session.getAccessToken())) {
            return false;
        }
        
        if (session.getSecurityFlags().isSuspiciousActivity()) {
            return false;
        }
        
        if (rateLimitService.isRateLimited(session.getSessionId(), "session_requests")) {
            markSuspiciousActivity(session);
            return false;
        }
        
        return true;
    }

    public void recordSuspiciousActivity(SessionData session, String reason) {
        session.getSecurityFlags().setSuspiciousActivity(true);
        session.getSecurityFlags().setFailedAttempts(
                session.getSecurityFlags().getFailedAttempts() + 1);
        session.getSecurityFlags().setLastSecurityCheck(Instant.now());
        
        sessionRepository.update(session);
        auditService.logSecurityEvent(session, "SUSPICIOUS_ACTIVITY", reason);
    }

    public boolean verifyDeviceFingerprint(SessionData session, String currentFingerprint) {
        if (!sessionProperties.getSecurity().isFingerprintVerification()) {
            return true;
        }
        
        return session.getDeviceFingerprint().equals(currentFingerprint);
    }

    private void markSuspiciousActivity(SessionData session) {
        session.getSecurityFlags().setSuspiciousActivity(true);
        sessionRepository.update(session);
    }
}

