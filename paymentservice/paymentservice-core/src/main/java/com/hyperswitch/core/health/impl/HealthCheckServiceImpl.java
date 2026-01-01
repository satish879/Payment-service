package com.hyperswitch.core.health.impl;

import com.hyperswitch.common.dto.HealthCheckResponse;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
import com.hyperswitch.core.health.HealthCheckService;
import io.r2dbc.spi.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of HealthCheckService
 * Note: This is created via @Bean in HealthCheckConfig, not via @Service
 */
public class HealthCheckServiceImpl implements HealthCheckService {
    
    private static final Logger log = LoggerFactory.getLogger(HealthCheckServiceImpl.class);
    private static final Duration HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(5);
    
    private final ConnectionFactory connectionFactory;
    private final ReactiveRedisConnectionFactory redisConnectionFactory;
    private final WebClient webClient;
    
    @Value("${hyperswitch.vault.url:}")
    private String vaultUrl;
    
    @Value("${hyperswitch.analytics.url:}")
    private String analyticsUrl;
    
    @Value("${hyperswitch.opensearch.url:}")
    private String opensearchUrl;
    
    @Value("${hyperswitch.decision-engine.url:}")
    private String decisionEngineUrl;
    
    @Value("${hyperswitch.unified-connector-service.url:}")
    private String unifiedConnectorServiceUrl;
    
    @Autowired
    public HealthCheckServiceImpl(
            ConnectionFactory connectionFactory, 
            ReactiveRedisConnectionFactory redisConnectionFactory,
            WebClient.Builder webClientBuilder) {
        this.connectionFactory = connectionFactory;
        this.redisConnectionFactory = redisConnectionFactory;
        this.webClient = webClientBuilder
            .baseUrl("http://localhost")
            .build();
        log.info("HealthCheckServiceImpl initialized");
    }
    
    @Override
    public Mono<Result<HealthCheckResponse, PaymentError>> performDeepHealthCheck() {
        log.info("Performing deep health check");
        
        HealthCheckResponse response = new HealthCheckResponse();
        
        // Check database
        Mono<Boolean> dbCheck = checkDatabase();
        
        // Check Redis
        Mono<Boolean> redisCheck = checkRedis();
        
        // Perform all health checks in parallel
        Mono<Boolean> vaultCheck = checkVault();
        Mono<Boolean> analyticsCheck = checkAnalytics();
        Mono<Boolean> opensearchCheck = checkOpenSearch();
        Mono<Boolean> outgoingRequestCheck = checkOutgoingRequest();
        Mono<Boolean> decisionEngineCheck = checkDecisionEngine();
        Mono<Boolean> unifiedConnectorServiceCheck = checkUnifiedConnectorService();
        
        return Mono.zip(
                dbCheck, 
                redisCheck, 
                vaultCheck, 
                analyticsCheck, 
                opensearchCheck, 
                outgoingRequestCheck,
                decisionEngineCheck,
                unifiedConnectorServiceCheck)
            .map(tuple -> {
                response.setDatabase(tuple.getT1());
                response.setRedis(tuple.getT2());
                response.setVault(tuple.getT3());
                response.setAnalytics(tuple.getT4());
                response.setOpensearch(tuple.getT5());
                response.setOutgoingRequest(tuple.getT6());
                
                Map<String, Boolean> grpcHealth = new HashMap<>();
                grpcHealth.put("dynamic_routing_service", Boolean.TRUE);
                response.setGrpcHealthCheck(grpcHealth);
                
                response.setDecisionEngine(tuple.getT7());
                response.setUnifiedConnectorService(tuple.getT8());
                
                // Determine overall status
                boolean allHealthy = Boolean.TRUE.equals(response.getDatabase()) 
                    && Boolean.TRUE.equals(response.getRedis())
                    && (response.getVault() == null || Boolean.TRUE.equals(response.getVault()))
                    && (response.getUnifiedConnectorService() == null || Boolean.TRUE.equals(response.getUnifiedConnectorService()));
                
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
    
    /**
     * Check vault service health
     * In production, this would make an HTTP request to vault health endpoint
     */
    private Mono<Boolean> checkVault() {
        if (vaultUrl == null || vaultUrl.isEmpty()) {
            log.debug("Vault URL not configured - skipping vault health check");
            return Mono.just(Boolean.TRUE); // Consider unconfigured as healthy
        }
        
        return webClient.mutate()
            .baseUrl(vaultUrl)
            .build()
            .get()
            .uri("/health")
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> Boolean.TRUE)
            .timeout(HEALTH_CHECK_TIMEOUT)
            .onErrorResume(error -> {
                log.warn("Vault health check failed: {}", error.getMessage());
                return Mono.just(Boolean.FALSE);
            })
            .defaultIfEmpty(Boolean.FALSE);
    }
    
    /**
     * Check analytics service health
     * In production, this would make an HTTP request to analytics health endpoint
     */
    private Mono<Boolean> checkAnalytics() {
        if (analyticsUrl == null || analyticsUrl.isEmpty()) {
            log.debug("Analytics URL not configured - skipping analytics health check");
            return Mono.just(Boolean.TRUE); // Consider unconfigured as healthy
        }
        
        return webClient.mutate()
            .baseUrl(analyticsUrl)
            .build()
            .get()
            .uri("/health")
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> Boolean.TRUE)
            .timeout(HEALTH_CHECK_TIMEOUT)
            .onErrorResume(error -> {
                log.warn("Analytics health check failed: {}", error.getMessage());
                return Mono.just(Boolean.FALSE);
            })
            .defaultIfEmpty(Boolean.FALSE);
    }
    
    /**
     * Check OpenSearch health
     * In production, this would make an HTTP request to OpenSearch health endpoint
     */
    private Mono<Boolean> checkOpenSearch() {
        if (opensearchUrl == null || opensearchUrl.isEmpty()) {
            log.debug("OpenSearch URL not configured - skipping OpenSearch health check");
            return Mono.just(Boolean.TRUE); // Consider unconfigured as healthy
        }
        
        return webClient.mutate()
            .baseUrl(opensearchUrl)
            .build()
            .get()
            .uri("/_cluster/health")
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> Boolean.TRUE)
            .timeout(HEALTH_CHECK_TIMEOUT)
            .onErrorResume(error -> {
                log.warn("OpenSearch health check failed: {}", error.getMessage());
                return Mono.just(Boolean.FALSE);
            })
            .defaultIfEmpty(Boolean.FALSE);
    }
    
    /**
     * Check outgoing request capability
     * In production, this would test making an HTTP request to an external service
     */
    private Mono<Boolean> checkOutgoingRequest() {
        // Test outgoing request capability by making a simple HTTP request
        // In production, this could ping a known external service or use a test endpoint
        return webClient.get()
            .uri("https://httpbin.org/get")
            .retrieve()
            .toBodilessEntity()
            .map(response -> response.getStatusCode().is2xxSuccessful())
            .timeout(HEALTH_CHECK_TIMEOUT)
            .onErrorResume(error -> {
                log.warn("Outgoing request health check failed: {}", error.getMessage());
                return Mono.just(Boolean.FALSE);
            })
            .defaultIfEmpty(Boolean.FALSE);
    }
    
    /**
     * Check decision engine health
     * In production, this would make an HTTP request to decision engine health endpoint
     */
    private Mono<Boolean> checkDecisionEngine() {
        if (decisionEngineUrl == null || decisionEngineUrl.isEmpty()) {
            log.debug("Decision engine URL not configured - skipping decision engine health check");
            return Mono.just(Boolean.TRUE); // Consider unconfigured as healthy
        }
        
        return webClient.mutate()
            .baseUrl(decisionEngineUrl)
            .build()
            .get()
            .uri("/health")
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> Boolean.TRUE)
            .timeout(HEALTH_CHECK_TIMEOUT)
            .onErrorResume(error -> {
                log.warn("Decision engine health check failed: {}", error.getMessage());
                return Mono.just(Boolean.FALSE);
            })
            .defaultIfEmpty(Boolean.FALSE);
    }
    
    /**
     * Check unified connector service health
     * In production, this would make an HTTP request to unified connector service health endpoint
     */
    private Mono<Boolean> checkUnifiedConnectorService() {
        if (unifiedConnectorServiceUrl == null || unifiedConnectorServiceUrl.isEmpty()) {
            log.debug("Unified connector service URL not configured - skipping health check");
            return Mono.just(Boolean.TRUE); // Consider unconfigured as healthy
        }
        
        return webClient.mutate()
            .baseUrl(unifiedConnectorServiceUrl)
            .build()
            .get()
            .uri("/health")
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> Boolean.TRUE)
            .timeout(HEALTH_CHECK_TIMEOUT)
            .onErrorResume(error -> {
                log.warn("Unified connector service health check failed: {}", error.getMessage());
                return Mono.just(Boolean.FALSE);
            })
            .defaultIfEmpty(Boolean.FALSE);
    }
}

