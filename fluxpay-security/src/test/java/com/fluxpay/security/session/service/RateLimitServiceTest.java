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

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(longs = {6L, 7L, 100L})
    void testIsRateLimited_ShouldReturnTrue_WhenAtOrOverLimit(long count) {
        when(valueOperations.increment(anyString())).thenReturn(count);

        boolean result = rateLimitService.isRateLimited("test-id", "session_creation");

        assertTrue(result);
    }

    @Test
    void testIsRateLimited_ShouldHandleNullCount() {
        when(valueOperations.increment(anyString())).thenReturn(null);

        boolean result = rateLimitService.isRateLimited("test-id", "session_creation");

        assertFalse(result);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "session_requests, 1001",
        "unknown_operation, 101"
    })
    void testIsRateLimited_ForDifferentOperations_ShouldReturnTrue_WhenOverLimit(String operation, long count) {
        when(valueOperations.increment(anyString())).thenReturn(count);

        boolean result = rateLimitService.isRateLimited("test-id", operation);

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

