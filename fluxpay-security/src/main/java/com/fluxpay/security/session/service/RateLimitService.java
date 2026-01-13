package com.fluxpay.security.session.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    public RateLimitService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isRateLimited(String identifier, String operation) {
        String key = "ratelimit:" + operation + ":" + identifier;
        Long count = redisTemplate.opsForValue().increment(key);
        long countValue = (count != null ? count : 0L);
        
        if (countValue == 1L) {
            redisTemplate.expire(key, getRateLimitWindow(operation), TimeUnit.SECONDS);
        }
        
        return countValue > getRateLimit(operation);
    }

    private int getRateLimit(String operation) {
        return switch (operation) {
            case "session_creation" -> 5;
            case "session_requests" -> 1000;
            default -> 100;
        };
    }

    private long getRateLimitWindow(String operation) {
        return Duration.ofMinutes(1).getSeconds();
    }
}

