package com.fluxpay.security.session.repository;

import com.fluxpay.security.session.model.SessionData;
import com.fluxpay.security.session.util.SessionTestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SessionRedisRepositoryTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @InjectMocks
    private SessionRedisRepository sessionRedisRepository;

    private SessionData testSession;
    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        testSession = SessionTestDataFactory.createSessionData(tenantId, userId);
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    void save_ShouldSaveSessionToRedis() {
        sessionRedisRepository.save(testSession);

        verify(valueOperations, times(2)).set(anyString(), any(), anyLong(), eq(TimeUnit.MINUTES));
        verify(setOperations).add(anyString(), eq(testSession.getSessionId()));
        verify(redisTemplate).expire(anyString(), anyLong(), eq(TimeUnit.MINUTES));
    }

    @Test
    void findBySessionId_ShouldReturnSession() {
        when(valueOperations.get(anyString())).thenReturn(testSession);

        SessionData result = sessionRedisRepository.findBySessionId(tenantId, userId, testSession.getSessionId());

        assertThat(result).isNotNull();
        assertThat(result.getSessionId()).isEqualTo(testSession.getSessionId());
    }

    @Test
    void findBySessionId_ShouldReturnNull_WhenNotFound() {
        when(valueOperations.get(anyString())).thenReturn(null);

        SessionData result = sessionRedisRepository.findBySessionId(tenantId, userId, "non-existent");

        assertThat(result).isNull();
    }

    @Test
    void findAllByUser_ShouldReturnAllSessions() {
        Set<Object> sessionIds = Set.of(testSession.getSessionId());
        when(setOperations.members(anyString())).thenReturn(sessionIds);
        when(valueOperations.get(anyString())).thenReturn(testSession);

        var sessions = sessionRedisRepository.findAllByUser(tenantId, userId);

        assertThat(sessions).hasSize(1);
    }

    @Test
    void countActiveSessions_ShouldReturnCount() {
        when(setOperations.size(anyString())).thenReturn(3L);

        long count = sessionRedisRepository.countActiveSessions(tenantId, userId);

        assertThat(count).isEqualTo(3L);
    }

    @Test
    void delete_ShouldRemoveSession() {
        sessionRedisRepository.delete(tenantId, userId, testSession.getSessionId());

        verify(redisTemplate).delete(anyString());
        verify(setOperations).remove(anyString(), eq(testSession.getSessionId()));
    }

    @Test
    void blacklistToken_ShouldAddToBlacklist() {
        Duration ttl = Duration.ofHours(1);

        sessionRedisRepository.blacklistToken("test-token", ttl);

        verify(valueOperations).set(anyString(), eq("blacklisted"), anyLong(), eq(TimeUnit.MINUTES));
    }

    @Test
    void isTokenBlacklisted_ShouldReturnTrue_WhenBlacklisted() {
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        boolean result = sessionRedisRepository.isTokenBlacklisted("test-token");

        assertThat(result).isTrue();
    }

    @Test
    void isTokenBlacklisted_ShouldReturnFalse_WhenNotBlacklisted() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        boolean result = sessionRedisRepository.isTokenBlacklisted("test-token");

        assertThat(result).isFalse();
    }

    @Test
    void update_ShouldUpdateSession() {
        testSession.setExpiresAt(Instant.now().plusSeconds(3600));

        sessionRedisRepository.update(testSession);

        verify(valueOperations).set(anyString(), eq(testSession), anyLong(), eq(TimeUnit.MINUTES));
    }

    @Test
    void findByRefreshToken_ShouldReturnSession() {
        String sessionKey = "session:tenant:user:" + testSession.getSessionId();
        when(valueOperations.get(eq("refresh:" + testSession.getRefreshToken()))).thenReturn(testSession.getSessionId());
        when(redisTemplate.keys(anyString())).thenReturn(Set.of(sessionKey));
        when(valueOperations.get(eq(sessionKey))).thenReturn(testSession);

        SessionData result = sessionRedisRepository.findByRefreshToken(testSession.getRefreshToken());

        assertThat(result).isNotNull();
        assertThat(result.getSessionId()).isEqualTo(testSession.getSessionId());
    }
}


