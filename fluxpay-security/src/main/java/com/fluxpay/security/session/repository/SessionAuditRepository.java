package com.fluxpay.security.session.repository;

import com.fluxpay.security.session.entity.SessionAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SessionAuditRepository extends JpaRepository<SessionAuditLog, UUID> {
    
    List<SessionAuditLog> findByUserIdAndTenantIdAndCreatedAtAfter(UUID userId, UUID tenantId, Instant after);
    
    List<SessionAuditLog> findBySessionId(String sessionId);
    
    void deleteByCreatedAtBefore(Instant before);
}

