package com.fluxpay.security.session.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testIsRateLimited_ShouldReturnFalse_WhenUnderLimit() {
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), anyLong(), any())).thenReturn(true);

        boolean result = rateLimitService.isRateLimited("test-id", "session_creation");

        assertFalse(result);
        verify(redisTemplate).expire(anyString(), eq(Duration.ofMinutes(1).getSeconds()), any());
    }

    @Test
    void testIsRateLimited_ShouldReturnTrue_WhenOverLimit() {
        when(valueOperations.increment(anyString())).thenReturn(6L);

        boolean result = rateLimitService.isRateLimited("test-id", "session_creation");

        assertTrue(result);
    }

    @Test
    void testIsRateLimited_ShouldReturnTrue_WhenAtLimit() {
        when(valueOperations.increment(anyString())).thenReturn(6L);

        boolean result = rateLimitService.isRateLimited("test-id", "session_creation");

        assertTrue(result);
    }

    @Test
    void testIsRateLimited_ShouldHandleNullCount() {
        when(valueOperations.increment(anyString())).thenReturn(null);

        boolean result = rateLimitService.isRateLimited("test-id", "session_creation");

        assertFalse(result);
    }

    @Test
    void testIsRateLimited_ForSessionRequests() {
        when(valueOperations.increment(anyString())).thenReturn(1001L);

        boolean result = rateLimitService.isRateLimited("test-id", "session_requests");

        assertTrue(result);
    }

    @Test
    void testIsRateLimited_ForDefaultOperation() {
        when(valueOperations.increment(anyString())).thenReturn(101L);

        boolean result = rateLimitService.isRateLimited("test-id", "unknown_operation");

        assertTrue(result);
    }

    @Test
    void testIsRateLimited_ShouldSetExpireOnlyOnFirstIncrement() {
        when(valueOperations.increment(anyString())).thenReturn(1L, 2L);
        when(redisTemplate.expire(anyString(), anyLong(), any())).thenReturn(true);

        rateLimitService.isRateLimited("test-id", "session_creation");
        rateLimitService.isRateLimited("test-id", "session_creation");

        verify(redisTemplate, times(1)).expire(anyString(), anyLong(), any());
    }
}

