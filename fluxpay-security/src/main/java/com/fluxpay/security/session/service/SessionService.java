package com.fluxpay.security.session.service;

import com.fluxpay.common.exception.SessionExpiredException;
import com.fluxpay.common.exception.SessionInvalidException;
import com.fluxpay.security.jwt.JwtTokenProvider;
import com.fluxpay.security.session.config.SessionProperties;
import com.fluxpay.security.session.model.SessionData;
import com.fluxpay.security.session.repository.SessionRedisRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class SessionService {

    private final SessionRedisRepository sessionRepository;
    private final SessionSecurityService securityService;
    private final SessionAuditService auditService;
    private final SessionProperties sessionProperties;
    private final JwtTokenProvider jwtTokenProvider;

    public SessionService(
            SessionRedisRepository sessionRepository,
            SessionSecurityService securityService,
            SessionAuditService auditService,
            SessionProperties sessionProperties,
            JwtTokenProvider jwtTokenProvider) {
        this.sessionRepository = sessionRepository;
        this.securityService = securityService;
        this.auditService = auditService;
        this.sessionProperties = sessionProperties;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public SessionData createSession(SessionData sessionData) {
        securityService.validateSessionCreation(sessionData);
        
        enforceConcurrentSessionLimit(sessionData.getTenantId(), sessionData.getUserId());
        
        sessionData.setExpiresAt(Instant.now().plus(sessionProperties.getTtl().getAccessToken()));
        sessionData.setRefreshTokenExpiresAt(Instant.now().plus(sessionProperties.getTtl().getRefreshToken()));
        
        sessionRepository.save(sessionData);
        auditService.logSessionCreated(sessionData);
        
        return sessionData;
    }

    public SessionData getSession(UUID tenantId, UUID userId, String sessionId) {
        SessionData session = sessionRepository.findBySessionId(tenantId, userId, sessionId);
        
        if (session == null) {
            return null;
        }
        
        if (!securityService.validateSession(session)) {
            invalidateSession(tenantId, userId, sessionId);
            return null;
        }
        
        return session;
    }

    @Async
    public CompletableFuture<Void> updateLastAccess(SessionData session) {
        session.setLastAccess(Instant.now());
        session.setRequestCount(session.getRequestCount() + 1);
        session.setLastRequestTime(Instant.now());
        
        sessionRepository.update(session);
        
        return CompletableFuture.completedFuture(null);
    }

    public void invalidateSession(UUID tenantId, UUID userId, String sessionId) {
        SessionData session = sessionRepository.findBySessionId(tenantId, userId, sessionId);
        
        if (session != null) {
            sessionRepository.delete(tenantId, userId, sessionId);
            blacklistTokens(session);
            auditService.logSessionTerminated(session, "Manual invalidation");
        }
    }

    public void invalidateAllUserSessions(UUID tenantId, UUID userId, String excludeSessionId) {
        List<SessionData> sessions = sessionRepository.findAllByUser(tenantId, userId);
        
        sessions.stream()
                .filter(s -> excludeSessionId == null || !s.getSessionId().equals(excludeSessionId))
                .forEach(s -> {
                    invalidateSession(tenantId, userId, s.getSessionId());
                });
    }

    public SessionData refreshSession(String refreshToken, String deviceFingerprint) {
        SessionData session = sessionRepository.findByRefreshToken(refreshToken);
        
        if (session == null || session.getRefreshTokenExpiresAt().isBefore(Instant.now())) {
            throw new SessionExpiredException("Refresh token expired or invalid");
        }
        
        if (!securityService.verifyDeviceFingerprint(session, deviceFingerprint)) {
            securityService.recordSuspiciousActivity(session, "Device fingerprint mismatch");
            invalidateSession(session.getTenantId(), session.getUserId(), session.getSessionId());
            throw new SessionInvalidException("Device verification failed");
        }
        
        String newRefreshToken = generateRefreshToken();
        session.setRefreshToken(newRefreshToken);
        session.setRefreshTokenExpiresAt(Instant.now().plus(sessionProperties.getTtl().getRefreshToken()));
        
        String newAccessToken = jwtTokenProvider.createToken(
                session.getUserId(), 
                session.getTenantId(), 
                session.getRole(), 
                session.getSessionId()
        );
        session.setAccessToken(newAccessToken);
        session.setExpiresAt(Instant.now().plus(sessionProperties.getTtl().getAccessToken()));
        
        sessionRepository.update(session);
        auditService.logTokenRefreshed(session);
        
        return session;
    }

    public List<SessionData> getUserSessions(UUID tenantId, UUID userId) {
        return sessionRepository.findAllByUser(tenantId, userId);
    }

    public boolean isTokenBlacklisted(String token) {
        return sessionRepository.isTokenBlacklisted(token);
    }

    private void enforceConcurrentSessionLimit(UUID tenantId, UUID userId) {
        long activeSessions = sessionRepository.countActiveSessions(tenantId, userId);
        int maxSessions = sessionProperties.getConcurrent().getMaxSessions();
        
        if (activeSessions >= maxSessions) {
            SessionData oldestSession = sessionRepository.findOldestSession(tenantId, userId);
            if (oldestSession != null) {
                invalidateSession(tenantId, userId, oldestSession.getSessionId());
            }
        }
    }

    private void blacklistTokens(SessionData session) {
        Duration accessTokenTtl = Duration.between(Instant.now(), session.getExpiresAt());
        Duration refreshTokenTtl = Duration.between(Instant.now(), session.getRefreshTokenExpiresAt());
        
        if (accessTokenTtl.isPositive()) {
            sessionRepository.blacklistToken(session.getAccessToken(), accessTokenTtl);
        }
        if (refreshTokenTtl.isPositive() && session.getRefreshToken() != null) {
            sessionRepository.blacklistToken(session.getRefreshToken(), refreshTokenTtl);
        }
    }

    private String generateRefreshToken() {
        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
    }
}

