package com.hyperswitch.web.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for reactive Redis operations
 */
@Configuration
@ConditionalOnClass(ReactiveRedisConnectionFactory.class)
public class RedisConfig {
    
    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);
    
    @Bean
    @ConditionalOnMissingBean(name = "reactiveRedisTemplate")
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        log.info("Creating ReactiveRedisTemplate<String, Object> bean");
        
        // Use String serializer for keys
        RedisSerializer<String> stringSerializer = new StringRedisSerializer();
        
        // Use GenericJackson2JsonRedisSerializer for values (Object)
        RedisSerializer<Object> jsonSerializer = RedisSerializer.json();
        
        // Create serialization context
        RedisSerializationContext<String, Object> serializationContext = 
            RedisSerializationContext.<String, Object>newSerializationContext()
                .key(stringSerializer)
                .value(jsonSerializer)
                .hashKey(stringSerializer)
                .hashValue(jsonSerializer)
                .build();
        
        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }
}

