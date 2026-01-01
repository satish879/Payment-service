package com.hyperswitch.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * REST controller for cache operations
 */
@RestController
@RequestMapping("/api/cache")
@Tag(name = "Cache", description = "Cache management operations")
public class CacheController {
    
    private static final Logger log = LoggerFactory.getLogger(CacheController.class);
    
    private ReactiveRedisTemplate<String, Object> redisTemplate;
    
    // Default constructor to allow bean creation even if dependencies are missing
    public CacheController() {
        log.warn("CacheController created without dependencies - Redis will be unavailable");
    }
    
    @Autowired(required = false)
    public void setRedisTemplate(ReactiveRedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        if (redisTemplate != null) {
            log.info("CacheController: Redis template injected successfully");
        }
    }
    
    private Mono<ResponseEntity<Map<String, String>>> checkRedisAvailable() {
        if (redisTemplate == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Redis service is not available")));
        }
        return null;
    }
    
    /**
     * Invalidate cache entry
     * POST /api/cache/invalidate/{key}
     */
    @PostMapping("/invalidate/{key}")
    @Operation(
        summary = "Invalidate cache entry",
        description = "Invalidates a cache entry by key"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cache entry invalidated successfully"
        ),
        @ApiResponse(
            responseCode = "503",
            description = "Redis service is not available"
        )
    })
    public Mono<ResponseEntity<Map<String, String>>> invalidateCache(@PathVariable("key") String key) {
        Mono<ResponseEntity<Map<String, String>>> unavailable = checkRedisAvailable();
        if (unavailable != null) return unavailable;
        
        return redisTemplate.delete(key)
            .map(deletedCount -> {
                Map<String, String> response = Map.of(
                    "key", key,
                    "status", deletedCount > 0 ? "invalidated" : "not_found"
                );
                return ResponseEntity.ok(response);
            })
            .defaultIfEmpty(ResponseEntity.ok(Map.of(
                "key", key,
                "status", "not_found"
            )));
    }
}

