package com.fluxpay.security.session.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fluxpay.security.session.entity.SessionAuditLog;
import com.fluxpay.security.session.model.SessionData;
import com.fluxpay.security.session.repository.SessionAuditRepository;
import com.fluxpay.security.session.util.SessionTestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SessionAuditServiceTest {

    @Mock
    private SessionAuditRepository auditRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SessionAuditService sessionAuditService;

    private SessionData testSession;

    @BeforeEach
    void setUp() {
        testSession = SessionTestDataFactory.createSessionData();
    }

    @Test
    void logSessionCreated_ShouldSaveAuditLog() throws Exception {
        ArgumentCaptor<SessionAuditLog> captor = ArgumentCaptor.forClass(SessionAuditLog.class);

        sessionAuditService.logSessionCreated(testSession).get();

        verify(auditRepository).save(captor.capture());
        SessionAuditLog log = captor.getValue();
        assertThat(log.getSessionId()).isEqualTo(testSession.getSessionId());
        assertThat(log.getUserId()).isEqualTo(testSession.getUserId());
        assertThat(log.getTenantId()).isEqualTo(testSession.getTenantId());
        assertThat(log.getEventType()).isEqualTo("SESSION_CREATED");
    }

    @Test
    void logSessionTerminated_ShouldSaveAuditLog() throws Exception {
        ArgumentCaptor<SessionAuditLog> captor = ArgumentCaptor.forClass(SessionAuditLog.class);

        sessionAuditService.logSessionTerminated(testSession, "Manual logout").get();

        verify(auditRepository).save(captor.capture());
        SessionAuditLog log = captor.getValue();
        assertThat(log.getEventType()).isEqualTo("SESSION_TERMINATED");
        assertThat(log.getDetails()).isEqualTo("Manual logout");
    }

    @Test
    void logTokenRefreshed_ShouldSaveAuditLog() throws Exception {
        ArgumentCaptor<SessionAuditLog> captor = ArgumentCaptor.forClass(SessionAuditLog.class);

        sessionAuditService.logTokenRefreshed(testSession).get();

        verify(auditRepository).save(captor.capture());
        SessionAuditLog log = captor.getValue();
        assertThat(log.getEventType()).isEqualTo("TOKEN_REFRESHED");
    }

    @Test
    void logSecurityEvent_ShouldSaveAuditLog() throws Exception {
        ArgumentCaptor<SessionAuditLog> captor = ArgumentCaptor.forClass(SessionAuditLog.class);

        sessionAuditService.logSecurityEvent(testSession, "SUSPICIOUS_LOGIN", "IP change detected").get();

        verify(auditRepository).save(captor.capture());
        SessionAuditLog log = captor.getValue();
        assertThat(log.getEventType()).isEqualTo("SUSPICIOUS_LOGIN");
        assertThat(log.getDetails()).isEqualTo("IP change detected");
    }
}


