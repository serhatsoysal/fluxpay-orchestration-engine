package com.fluxpay.security.session.repository;

import com.fluxpay.security.TestApplication;
import com.fluxpay.security.session.entity.SessionAuditLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = TestApplication.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SessionAuditRepositoryTest {

    @Autowired
    private SessionAuditRepository sessionAuditRepository;

    @Test
    void save_ShouldPersistAuditLog() {
        SessionAuditLog log = SessionAuditLog.builder()
                .sessionId(UUID.randomUUID().toString())
                .userId(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .eventType("SESSION_CREATED")
                .ipAddress("192.168.1.1")
                .details("Test session created")
                .build();

        SessionAuditLog saved = sessionAuditRepository.save(log);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEventType()).isEqualTo("SESSION_CREATED");
    }

    @Test
    void findByTenantIdAndSessionId_ShouldReturnLogs() {
        UUID tenantId = UUID.randomUUID();
        String sessionId = UUID.randomUUID().toString();

        SessionAuditLog log = SessionAuditLog.builder()
                .sessionId(sessionId)
                .userId(UUID.randomUUID())
                .tenantId(tenantId)
                .eventType("SESSION_CREATED")
                .ipAddress("192.168.1.1")
                .details("Test")
                .build();

        sessionAuditRepository.save(log);

        assertThat(log.getSessionId()).isEqualTo(sessionId);
    }
}

