package com.hyperswitch.core.health.impl;

import com.hyperswitch.common.dto.HealthCheckResponse;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
import com.hyperswitch.core.health.HealthCheckService;
import io.r2dbc.spi.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of HealthCheckService
 */
public class HealthCheckServiceImpl implements HealthCheckService {
    
    private static final Logger log = LoggerFactory.getLogger(HealthCheckServiceImpl.class);
    
    private final ConnectionFactory connectionFactory;
    private final ReactiveRedisConnectionFactory redisConnectionFactory;
    
    public HealthCheckServiceImpl(ConnectionFactory connectionFactory, ReactiveRedisConnectionFactory redisConnectionFactory) {
        this.connectionFactory = connectionFactory;
        this.redisConnectionFactory = redisConnectionFactory;
        log.error("=== HealthCheckServiceImpl BEAN CREATED ===");
        log.error("ConnectionFactory: {}", connectionFactory != null ? "available" : "null");
        log.error("ReactiveRedisConnectionFactory: {}", redisConnectionFactory != null ? "available" : "null");
    }
    
    @Override
    public Mono<Result<HealthCheckResponse, PaymentError>> performDeepHealthCheck() {
        log.info("Performing deep health check");
        
        HealthCheckResponse response = new HealthCheckResponse();
        
        // Check database
        Mono<Boolean> dbCheck = checkDatabase();
        
        // Check Redis
        Mono<Boolean> redisCheck = checkRedis();
        
        return Mono.zip(dbCheck, redisCheck)
            .map(tuple -> {
                response.setDatabase(tuple.getT1());
                response.setRedis(tuple.getT2());
                response.setVault(Boolean.TRUE); // Vault check would be implemented in production
                response.setAnalytics(Boolean.TRUE); // Analytics check would be implemented in production
                response.setOpensearch(Boolean.TRUE); // OpenSearch check would be implemented in production
                response.setOutgoingRequest(Boolean.TRUE); // Outgoing request check would be implemented in production
                
                Map<String, Boolean> grpcHealth = new HashMap<>();
                grpcHealth.put("dynamic_routing_service", Boolean.TRUE);
                response.setGrpcHealthCheck(grpcHealth);
                
                response.setDecisionEngine(Boolean.TRUE); // Decision engine check would be implemented in production
                response.setUnifiedConnectorService(Boolean.TRUE); // Unified connector service check would be implemented in production
                
                // Determine overall status
                boolean allHealthy = response.getDatabase() 
                    && response.getRedis() 
                    && (response.getVault() == null || response.getVault())
                    && (response.getUnifiedConnectorService() == null || response.getUnifiedConnectorService());
                
                response.setStatus(allHealthy ? "healthy" : "degraded");
                
                return Result.<HealthCheckResponse, PaymentError>ok(response);
            })
            .onErrorResume(error -> {
                log.error("Error performing deep health check: {}", error.getMessage(), error);
                response.setStatus("unhealthy");
                return Mono.just(Result.<HealthCheckResponse, PaymentError>ok(response));
            });
    }
    
    @Override
    public Mono<Result<HealthCheckResponse, PaymentError>> performHealthCheck() {
        log.info("Performing basic health check");
        
        return performDeepHealthCheck();
    }
    
    private Mono<Boolean> checkDatabase() {
        if (connectionFactory == null) {
            log.warn("ConnectionFactory is null - cannot check database");
            return Mono.just(Boolean.FALSE);
        }
        return Mono.from(connectionFactory.create())
            .flatMap(connection -> 
                Mono.from(connection.createStatement("SELECT 1").execute())
                    .flatMap(result -> Mono.from(result.getRowsUpdated()))
                    .thenReturn(Boolean.TRUE)
                    .doFinally(signalType -> Mono.from(connection.close()).subscribe())
            )
            .onErrorReturn(Boolean.FALSE)
            .defaultIfEmpty(Boolean.FALSE);
    }
    
    private Mono<Boolean> checkRedis() {
        if (redisConnectionFactory == null) {
            log.warn("ReactiveRedisConnectionFactory is null - cannot check Redis");
            return Mono.just(Boolean.FALSE);
        }
        return redisConnectionFactory.getReactiveConnection()
            .ping()
            .thenReturn(Boolean.TRUE)
            .onErrorReturn(Boolean.FALSE)
            .defaultIfEmpty(Boolean.FALSE);
    }
}

