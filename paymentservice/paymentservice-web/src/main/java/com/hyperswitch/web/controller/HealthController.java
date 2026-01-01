package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.HealthCheckResponse;
import com.hyperswitch.core.health.HealthCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Health check endpoints
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Health", description = "Health check operations")
public class HealthController {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);
    
    @Autowired(required = false)
    private HealthCheckService healthCheckService;
    
    public HealthController() {
        // Default constructor to allow bean creation even if HealthCheckService is not available
    }
    
    @PostConstruct
    public void init() {
        logger.info("=== HealthController BEAN CREATED ===");
        logger.info("HealthCheckService available: {}", healthCheckService != null);
        logger.info("Health endpoints registered: /api/health, /api/health/ready, /api/v2/health, /api/v2/health/ready");
    }
    
    /**
     * Basic health check
     * GET /health
     */
    @GetMapping("/health")
    @Operation(
        summary = "Basic health check",
        description = "Returns basic health status of the service"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Service is healthy"
        )
    })
    public Mono<ResponseEntity<Map<String, String>>> health() {
        logger.debug("Health endpoint called");
        return Mono.just(ResponseEntity.ok(Map.of("status", "healthy", "service", "hyperswitch-payment-service")));
    }
    
    /**
     * Deep health check
     * GET /health/ready
     */
    @GetMapping("/health/ready")
    @Operation(
        summary = "Deep health check",
        description = "Returns comprehensive health status of all components"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Health check completed",
            content = @Content(schema = @Schema(implementation = HealthCheckResponse.class))
        )
    })
    public Mono<ResponseEntity<HealthCheckResponse>> deepHealthCheck() {
        if (healthCheckService == null) {
            logger.warn("HealthCheckService not available - returning basic health status");
            HealthCheckResponse response = new HealthCheckResponse();
            response.setStatus("healthy");
            return Mono.just(ResponseEntity.ok(response));
        }
        return healthCheckService.performDeepHealthCheck()
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    HealthCheckResponse errorResponse = new HealthCheckResponse();
                    errorResponse.setStatus("unhealthy");
                    return ResponseEntity.ok(errorResponse);
                }
            });
    }
    
    /**
     * Health check (v2 API)
     * GET /v2/health
     */
    @GetMapping("/v2/health")
    @Operation(
        summary = "Health check (v2)",
        description = "Returns health status using v2 API"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Health check completed",
            content = @Content(schema = @Schema(implementation = HealthCheckResponse.class))
        )
    })
    public Mono<ResponseEntity<HealthCheckResponse>> healthV2() {
        if (healthCheckService == null) {
            logger.warn("HealthCheckService not available - returning basic health status");
            HealthCheckResponse response = new HealthCheckResponse();
            response.setStatus("healthy");
            return Mono.just(ResponseEntity.ok(response));
        }
        return healthCheckService.performHealthCheck()
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    HealthCheckResponse errorResponse = new HealthCheckResponse();
                    errorResponse.setStatus("unhealthy");
                    return ResponseEntity.ok(errorResponse);
                }
            });
    }
    
    /**
     * Deep health check (v2 API)
     * GET /v2/health/ready
     */
    @GetMapping("/v2/health/ready")
    @Operation(
        summary = "Deep health check (v2)",
        description = "Returns comprehensive health status of all components using v2 API"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Health check completed",
            content = @Content(schema = @Schema(implementation = HealthCheckResponse.class))
        )
    })
    public Mono<ResponseEntity<HealthCheckResponse>> deepHealthCheckV2() {
        if (healthCheckService == null) {
            logger.warn("HealthCheckService not available - returning basic health status");
            HealthCheckResponse response = new HealthCheckResponse();
            response.setStatus("healthy");
            return Mono.just(ResponseEntity.ok(response));
        }
        return healthCheckService.performDeepHealthCheck()
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    HealthCheckResponse errorResponse = new HealthCheckResponse();
                    errorResponse.setStatus("unhealthy");
                    return ResponseEntity.ok(errorResponse);
                }
            });
    }
}

