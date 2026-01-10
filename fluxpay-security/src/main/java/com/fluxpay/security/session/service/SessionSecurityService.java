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
        if (session == null) {
            throw new IllegalArgumentException("Session data cannot be null");
        }
        
        if (session.getIpAddress() != null && rateLimitService.isRateLimited(session.getIpAddress(), "session_creation")) {
            throw new RateLimitExceededException("Too many session creation attempts");
        }
    }

    public boolean validateSession(SessionData session) {
        if (session == null) {
            return false;
        }
        
        if (session.getExpiresAt() == null || session.getExpiresAt().isBefore(Instant.now())) {
            return false;
        }
        
        if (session.getAccessToken() != null && sessionRepository.isTokenBlacklisted(session.getAccessToken())) {
            return false;
        }
        
        if (session.getSecurityFlags() != null && session.getSecurityFlags().isSuspiciousActivity()) {
            return false;
        }
        
        if (session.getSessionId() != null && rateLimitService.isRateLimited(session.getSessionId(), "session_requests")) {
            markSuspiciousActivity(session);
            return false;
        }
        
        return true;
    }

    public void recordSuspiciousActivity(SessionData session, String reason) {
        if (session == null) {
            return;
        }
        
        if (session.getSecurityFlags() != null) {
            session.getSecurityFlags().setSuspiciousActivity(true);
            session.getSecurityFlags().setFailedAttempts(
                    session.getSecurityFlags().getFailedAttempts() + 1);
            session.getSecurityFlags().setLastSecurityCheck(Instant.now());
        }
        
        sessionRepository.update(session);
        auditService.logSecurityEvent(session, "SUSPICIOUS_ACTIVITY", reason);
    }

    public boolean verifyDeviceFingerprint(SessionData session, String currentFingerprint) {
        if (session == null) {
            return false;
        }
        
        if (sessionProperties.getSecurity() == null || !sessionProperties.getSecurity().isFingerprintVerification()) {
            return true;
        }
        
        if (session.getDeviceFingerprint() == null || currentFingerprint == null) {
            return false;
        }
        
        return session.getDeviceFingerprint().equals(currentFingerprint);
    }

    private void markSuspiciousActivity(SessionData session) {
        if (session != null && session.getSecurityFlags() != null) {
            session.getSecurityFlags().setSuspiciousActivity(true);
            sessionRepository.update(session);
        }
    }
}

