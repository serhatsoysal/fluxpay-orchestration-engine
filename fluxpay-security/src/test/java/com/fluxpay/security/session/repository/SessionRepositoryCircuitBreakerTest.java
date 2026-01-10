package com.fluxpay.security.session.repository;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class SessionRepositoryCircuitBreakerTest {

    @Test
    void circuitBreaker_ShouldHaveFallbackMethods() throws Exception {
        Class<?> clazz = SessionRedisRepository.class;
        
        Method saveFallback = clazz.getDeclaredMethod("saveFallback", 
                com.fluxpay.security.session.model.SessionData.class, Exception.class);
        assertThat(saveFallback).isNotNull();
        
        Method findBySessionIdFallback = clazz.getDeclaredMethod("findBySessionIdFallback", 
                java.util.UUID.class, java.util.UUID.class, String.class, Exception.class);
        assertThat(findBySessionIdFallback).isNotNull();
    }
}


