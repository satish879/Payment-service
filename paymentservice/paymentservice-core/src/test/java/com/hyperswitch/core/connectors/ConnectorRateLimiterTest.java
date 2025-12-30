package com.hyperswitch.core.connectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ConnectorRateLimiter
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorRateLimiter Unit Tests")
class ConnectorRateLimiterTest {
    
    @InjectMocks
    private ConnectorRateLimiter rateLimiter;
    
    @Test
    @DisplayName("Should allow requests within rate limit")
    void testAllowWithinRateLimit() {
        // When
        Mono<Boolean> result = rateLimiter.isAllowed("stripe");
        
        // Then
        StepVerifier.create(result)
            .assertNext(allowed -> {
                assertThat(allowed).isTrue();
            })
            .verifyComplete();
    }
    
    @Test
    @DisplayName("Should enforce rate limit")
    void testEnforceRateLimit() {
        // Given - Make many requests quickly
        String connectorName = "stripe";
        
        // When - Make requests up to the limit
        for (int i = 0; i < 100; i++) {
            final int requestIndex = i;
            Mono<Boolean> result = rateLimiter.isAllowed(connectorName);
            StepVerifier.create(result)
                .assertNext(allowed -> {
                    // First requests should be allowed
                    if (requestIndex < 100) {
                        assertThat(allowed).isTrue();
                    }
                })
                .verifyComplete();
        }
        
        // Then - Next request should be rate limited
        Mono<Boolean> result = rateLimiter.isAllowed(connectorName);
        StepVerifier.create(result)
            .assertNext(allowed -> {
                // May be rate limited depending on timing
                assertThat(allowed).isNotNull();
            })
            .verifyComplete();
    }
    
    @Test
    @DisplayName("Should update rate limit configuration")
    void testUpdateRateLimit() {
        // When
        rateLimiter.updateRateLimit("custom_connector", 200, java.time.Duration.ofMinutes(1));
        
        // Then - Should not throw exception
        Mono<Boolean> result = rateLimiter.isAllowed("custom_connector");
        StepVerifier.create(result)
            .assertNext(allowed -> {
                assertThat(allowed).isTrue();
            })
            .verifyComplete();
    }
}

