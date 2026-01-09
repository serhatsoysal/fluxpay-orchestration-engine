package com.fluxpay.security.session.repository;

import com.fluxpay.security.session.model.SessionData;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Repository
public class SessionRedisRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionRedisRepository.class);

    private static final String SESSION_PREFIX = "session:";
    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String USER_SESSIONS_PREFIX = "user_sessions:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    
    private final RedisTemplate<String, Object> redisTemplate;

    public SessionRedisRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @CircuitBreaker(name = "sessionRepository", fallbackMethod = "saveFallback")
    @Retry(name = "sessionRepository")
    public void save(SessionData session) {
        String key = buildSessionKey(session.getTenantId(), session.getUserId(), session.getSessionId());
        Duration ttl = Duration.between(java.time.Instant.now(), session.getExpiresAt());
        
        redisTemplate.opsForValue().set(key, session, ttl.toMinutes(), TimeUnit.MINUTES);
        
        String userSessionsKey = buildUserSessionsKey(session.getTenantId(), session.getUserId());
        redisTemplate.opsForSet().add(userSessionsKey, session.getSessionId());
        redisTemplate.expire(userSessionsKey, ttl.toMinutes(), TimeUnit.MINUTES);
        
        String refreshKey = REFRESH_TOKEN_PREFIX + session.getRefreshToken();
        redisTemplate.opsForValue().set(refreshKey, session.getSessionId(), 
                Duration.between(java.time.Instant.now(), session.getRefreshTokenExpiresAt()).toMinutes(), TimeUnit.MINUTES);
    }

    @CircuitBreaker(name = "sessionRepository", fallbackMethod = "findBySessionIdFallback")
    public SessionData findBySessionId(UUID tenantId, UUID userId, String sessionId) {
        String key = buildSessionKey(tenantId, userId, sessionId);
        return (SessionData) redisTemplate.opsForValue().get(key);
    }

    public List<SessionData> findAllByUser(UUID tenantId, UUID userId) {
        String userSessionsKey = buildUserSessionsKey(tenantId, userId);
        Set<Object> sessionIds = redisTemplate.opsForSet().members(userSessionsKey);
        
        if (sessionIds == null) {
            return new ArrayList<>();
        }
        
        return sessionIds.stream()
                .map(id -> findBySessionId(tenantId, userId, id.toString()))
                .filter(session -> session != null)
                .toList();
    }

    public long countActiveSessions(UUID tenantId, UUID userId) {
        String userSessionsKey = buildUserSessionsKey(tenantId, userId);
        Long count = redisTemplate.opsForSet().size(userSessionsKey);
        return count != null ? count : 0;
    }

    public SessionData findOldestSession(UUID tenantId, UUID userId) {
        List<SessionData> sessions = findAllByUser(tenantId, userId);
        return sessions.stream()
                .min((s1, s2) -> s1.getCreatedAt().compareTo(s2.getCreatedAt()))
                .orElse(null);
    }

    public void delete(UUID tenantId, UUID userId, String sessionId) {
        String key = buildSessionKey(tenantId, userId, sessionId);
        SessionData session = (SessionData) redisTemplate.opsForValue().get(key);
        
        redisTemplate.delete(key);
        
        String userSessionsKey = buildUserSessionsKey(tenantId, userId);
        redisTemplate.opsForSet().remove(userSessionsKey, sessionId);
        
        if (session != null && session.getRefreshToken() != null) {
            String refreshKey = REFRESH_TOKEN_PREFIX + session.getRefreshToken();
            redisTemplate.delete(refreshKey);
        }
    }

    public void blacklistToken(String token, Duration ttl) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "blacklisted", ttl.toMinutes(), TimeUnit.MINUTES);
    }

    public boolean isTokenBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public SessionData findByRefreshToken(String refreshToken) {
        String refreshKey = REFRESH_TOKEN_PREFIX + refreshToken;
        String sessionId = (String) redisTemplate.opsForValue().get(refreshKey);
        
        if (sessionId == null) {
            return null;
        }
        
        Set<String> keys = redisTemplate.keys(SESSION_PREFIX + "*:" + sessionId);
        if (keys == null || keys.isEmpty()) {
            return null;
        }
        
        return (SessionData) redisTemplate.opsForValue().get(keys.iterator().next());
    }

    public void update(SessionData session) {
        save(session);
    }

    private void saveFallback(SessionData session, Exception ex) {
        LOGGER.warn("Session save fallback invoked for session: {} due to exception: {}", 
                session != null ? session.getSessionId() : "null", ex.getMessage());
    }

    @SuppressWarnings("unused")
    private SessionData findBySessionIdFallback(UUID tenantId, UUID userId, String sessionId, Exception ex) {
        LOGGER.warn("Session find fallback invoked for tenantId: {}, userId: {}, sessionId: {} due to exception: {}", 
                tenantId, userId, sessionId, ex.getMessage());
        return null;
    }

    private String buildSessionKey(UUID tenantId, UUID userId, String sessionId) {
        return SESSION_PREFIX + tenantId + ":" + userId + ":" + sessionId;
    }

    private String buildUserSessionsKey(UUID tenantId, UUID userId) {
        return USER_SESSIONS_PREFIX + tenantId + ":" + userId;
    }
}

