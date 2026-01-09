package com.fluxpay.security.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.assertj.core.api.Assertions.assertThat;

class RedisConfigTest {

    @Test
    void redisConfig_ShouldCreateRedisTemplate() {
        RedisConnectionFactory connectionFactory = new LettuceConnectionFactory("localhost", 6379);
        RedisConfig config = new RedisConfig();
        RedisTemplate<String, Object> redisTemplate = config.redisTemplate(connectionFactory);

        assertThat(redisTemplate).isNotNull();
        assertThat(redisTemplate.getConnectionFactory()).isNotNull();
        assertThat(redisTemplate.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(redisTemplate.getValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
    }
}


