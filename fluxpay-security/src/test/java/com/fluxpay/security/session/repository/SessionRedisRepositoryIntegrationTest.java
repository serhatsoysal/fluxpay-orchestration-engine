package com.fluxpay.security.session.repository;

import com.fluxpay.security.session.config.EmbeddedRedisConfig;
import com.fluxpay.security.session.model.SessionData;
import com.fluxpay.security.session.util.SessionTestDataFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {com.fluxpay.security.config.RedisConfig.class, EmbeddedRedisConfig.class, SessionRedisRepository.class})
@TestPropertySource(properties = {
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.main.allow-bean-definition-overriding=true"
})
@Disabled("Requires Docker. Enable manually when Docker is available.")
class SessionRedisRepositoryIntegrationTest {

    @Autowired
    private SessionRedisRepository sessionRedisRepository;

    @Test
    void shouldSaveAndRetrieveSession() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SessionData session = SessionTestDataFactory.createSessionData(tenantId, userId);

        sessionRedisRepository.save(session);

        SessionData retrieved = sessionRedisRepository.findBySessionId(tenantId, userId, session.getSessionId());

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getSessionId()).isEqualTo(session.getSessionId());
        assertThat(retrieved.getUserId()).isEqualTo(userId);
    }

    @Test
    void shouldDeleteSession() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SessionData session = SessionTestDataFactory.createSessionData(tenantId, userId);

        sessionRedisRepository.save(session);
        sessionRedisRepository.delete(tenantId, userId, session.getSessionId());

        SessionData retrieved = sessionRedisRepository.findBySessionId(tenantId, userId, session.getSessionId());

        assertThat(retrieved).isNull();
    }

    @Test
    void shouldCountActiveSessions() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SessionData session1 = SessionTestDataFactory.createSessionData(tenantId, userId);
        SessionData session2 = SessionTestDataFactory.createSessionData(tenantId, userId);

        sessionRedisRepository.save(session1);
        sessionRedisRepository.save(session2);

        long count = sessionRedisRepository.countActiveSessions(tenantId, userId);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldBlacklistToken() {
        String token = "test-token";

        sessionRedisRepository.blacklistToken(token, Duration.ofMinutes(5));

        boolean isBlacklisted = sessionRedisRepository.isTokenBlacklisted(token);

        assertThat(isBlacklisted).isTrue();
    }

    @Test
    void shouldFindOldestSession() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SessionData session1 = SessionTestDataFactory.createSessionData(tenantId, userId);
        SessionData session2 = SessionTestDataFactory.createSessionData(tenantId, userId);
        
        session1.setCreatedAt(java.time.Instant.now().minusSeconds(3600));
        session2.setCreatedAt(java.time.Instant.now());

        sessionRedisRepository.save(session1);
        sessionRedisRepository.save(session2);

        SessionData oldest = sessionRedisRepository.findOldestSession(tenantId, userId);

        assertThat(oldest).isNotNull();
        assertThat(oldest.getSessionId()).isEqualTo(session1.getSessionId());
    }

    @Test
    void shouldUpdateSession() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SessionData session = SessionTestDataFactory.createSessionData(tenantId, userId);

        sessionRedisRepository.save(session);
        session.setRequestCount(10);
        sessionRedisRepository.update(session);

        SessionData retrieved = sessionRedisRepository.findBySessionId(tenantId, userId, session.getSessionId());

        assertThat(retrieved.getRequestCount()).isEqualTo(10);
    }

    @Test
    void shouldFindByRefreshToken() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SessionData session = SessionTestDataFactory.createSessionData(tenantId, userId);

        sessionRedisRepository.save(session);

        SessionData retrieved = sessionRedisRepository.findByRefreshToken(session.getRefreshToken());

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getRefreshToken()).isEqualTo(session.getRefreshToken());
    }
}


