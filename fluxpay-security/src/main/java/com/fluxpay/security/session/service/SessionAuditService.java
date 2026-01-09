package com.fluxpay.security.session.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fluxpay.security.session.entity.SessionAuditLog;
import com.fluxpay.security.session.model.SessionData;
import com.fluxpay.security.session.repository.SessionAuditRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class SessionAuditService {

    private final SessionAuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    public SessionAuditService(SessionAuditRepository auditRepository, ObjectMapper objectMapper) {
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
    }

    @Async
    public CompletableFuture<Void> logSessionCreated(SessionData session) {
        SessionAuditLog log = SessionAuditLog.builder()
                .sessionId(session.getSessionId())
                .userId(session.getUserId())
                .tenantId(session.getTenantId())
                .eventType("SESSION_CREATED")
                .ipAddress(session.getIpAddress())
                .deviceInfo(serializeDeviceInfo(session))
                .details("Session created")
                .build();
        
        auditRepository.save(log);
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> logSessionTerminated(SessionData session, String reason) {
        SessionAuditLog log = SessionAuditLog.builder()
                .sessionId(session.getSessionId())
                .userId(session.getUserId())
                .tenantId(session.getTenantId())
                .eventType("SESSION_TERMINATED")
                .ipAddress(session.getIpAddress())
                .deviceInfo(serializeDeviceInfo(session))
                .details(reason)
                .build();
        
        auditRepository.save(log);
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> logTokenRefreshed(SessionData session) {
        SessionAuditLog log = SessionAuditLog.builder()
                .sessionId(session.getSessionId())
                .userId(session.getUserId())
                .tenantId(session.getTenantId())
                .eventType("TOKEN_REFRESHED")
                .ipAddress(session.getIpAddress())
                .deviceInfo(serializeDeviceInfo(session))
                .details("Access token refreshed")
                .build();
        
        auditRepository.save(log);
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> logSecurityEvent(SessionData session, String eventType, String details) {
        SessionAuditLog log = SessionAuditLog.builder()
                .sessionId(session.getSessionId())
                .userId(session.getUserId())
                .tenantId(session.getTenantId())
                .eventType(eventType)
                .ipAddress(session.getIpAddress())
                .deviceInfo(serializeDeviceInfo(session))
                .details(details)
                .build();
        
        auditRepository.save(log);
        return CompletableFuture.completedFuture(null);
    }

    private String serializeDeviceInfo(SessionData session) {
        try {
            return objectMapper.writeValueAsString(session.getDeviceInfo());
        } catch (JsonProcessingException e) {
            return session.getDeviceInfo() != null ? session.getDeviceInfo().toString() : "";
        }
    }
}

