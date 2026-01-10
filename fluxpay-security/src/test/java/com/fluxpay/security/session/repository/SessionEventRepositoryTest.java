package com.fluxpay.security.session.repository;

import com.fluxpay.security.TestApplication;
import com.fluxpay.security.session.entity.SessionEvent;
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
class SessionEventRepositoryTest {

    @Autowired
    private SessionEventRepository sessionEventRepository;

    @Test
    void save_ShouldPersistEvent() {
        SessionEvent event = SessionEvent.builder()
                .sessionId(UUID.randomUUID().toString())
                .userId(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .eventType("LOGIN")
                .build();

        SessionEvent saved = sessionEventRepository.save(event);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEventType()).isEqualTo("LOGIN");
    }

    @Test
    void findByTenantId_ShouldReturnEvents() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        SessionEvent event = SessionEvent.builder()
                .sessionId(UUID.randomUUID().toString())
                .userId(userId)
                .tenantId(tenantId)
                .eventType("LOGIN")
                .build();

        SessionEvent saved = sessionEventRepository.save(event);

        assertThat(saved).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(userId);
    }

    @Test
    void findBySessionId_ShouldReturnEvent() {
        String sessionId = UUID.randomUUID().toString();

        SessionEvent event = SessionEvent.builder()
                .sessionId(sessionId)
                .userId(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .eventType("ACCESS")
                .build();

        SessionEvent saved = sessionEventRepository.save(event);

        assertThat(saved).isNotNull();
        assertThat(saved.getSessionId()).isEqualTo(sessionId);
    }
}


