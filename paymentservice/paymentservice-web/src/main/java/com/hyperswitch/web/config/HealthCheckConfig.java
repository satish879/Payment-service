package com.hyperswitch.web.config;

import com.hyperswitch.core.health.HealthCheckService;
import com.hyperswitch.core.health.impl.HealthCheckServiceImpl;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;

/**
 * Configuration for HealthCheckService
 * Explicitly creates the bean to ensure it's available even if dependencies are optional
 */
@Configuration
public class HealthCheckConfig {
    
    private final ApplicationContext applicationContext;
    
    public HealthCheckConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    @Bean
    @ConditionalOnMissingBean(HealthCheckService.class)
    public HealthCheckService healthCheckService() {
        // Get dependencies from context if available, otherwise null
        ConnectionFactory connectionFactory = null;
        ReactiveRedisConnectionFactory redisConnectionFactory = null;
        
        try {
            connectionFactory = applicationContext.getBean(ConnectionFactory.class);
        } catch (Exception _) {
            // Bean not available, will be null
        }
        
        try {
            redisConnectionFactory = applicationContext.getBean(ReactiveRedisConnectionFactory.class);
        } catch (Exception _) {
            // Bean not available, will be null
        }
        
        // Pass dependencies to constructor - they can be null if not available
        return new HealthCheckServiceImpl(connectionFactory, redisConnectionFactory);
    }
}

