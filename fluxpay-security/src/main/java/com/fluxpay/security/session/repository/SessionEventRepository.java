package com.fluxpay.security.session.repository;

import com.fluxpay.security.session.entity.SessionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SessionEventRepository extends JpaRepository<SessionEvent, UUID> {
    
    List<SessionEvent> findByUserIdAndTenantIdAndTimestampAfter(UUID userId, UUID tenantId, Instant after);
    
    List<SessionEvent> findBySessionId(String sessionId);
}

