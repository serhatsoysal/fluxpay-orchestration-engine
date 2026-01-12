package com.fluxpay.security.session.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@TestConfiguration
public class EmbeddedRedisConfig {

    private GenericContainer<?> redisContainer;

    @PostConstruct
    @SuppressWarnings("resource")
    public void startRedis() {
        try {
            redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);
            redisContainer.start();
        } catch (Exception e) {
            // Docker not available, skip container startup
            // Tests using this config should be conditionally enabled
            redisContainer = null;
        }
    }

    @PreDestroy
    public void stopRedis() {
        if (redisContainer != null) {
            try {
                redisContainer.stop();
            } catch (Exception e) {
                // Ignore errors during shutdown
            }
        }
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        String host = redisContainer != null ? redisContainer.getHost() : "localhost";
        Integer mappedPort = redisContainer != null ? redisContainer.getMappedPort(6379) : null;
        int port = mappedPort != null ? mappedPort : 6379;
        LettuceConnectionFactory factory = new LettuceConnectionFactory(host, port);
        factory.afterPropertiesSet();
        return factory;
    }
}