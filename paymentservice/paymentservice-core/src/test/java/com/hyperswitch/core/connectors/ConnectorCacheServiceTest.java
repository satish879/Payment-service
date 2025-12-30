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
 * Unit tests for ConnectorCacheService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorCacheService Unit Tests")
class ConnectorCacheServiceTest {
    
    @InjectMocks
    private ConnectorCacheService cacheService;
    
    private String testKey;
    private String testValue;
    
    @BeforeEach
    void setUp() {
        testKey = "test_key";
        testValue = "test_value";
    }
    
    @Test
    @DisplayName("Should cache and retrieve value")
    void testCacheAndRetrieve() {
        // Given
        cacheService.put(testKey, testValue);
        
        // When
        Mono<String> result = cacheService.getCached(testKey, String.class);
        
        // Then
        StepVerifier.create(result)
            .assertNext(value -> {
                assertThat(value).isEqualTo(testValue);
            })
            .verifyComplete();
    }
    
    @Test
    @DisplayName("Should return empty for non-existent key")
    void testGetNonExistentKey() {
        // When
        Mono<String> result = cacheService.getCached("non_existent", String.class);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
    }
    
    @Test
    @DisplayName("Should invalidate cache entry")
    void testInvalidateCache() {
        // Given
        cacheService.put(testKey, testValue);
        
        // When
        cacheService.invalidate(testKey);
        
        // Then
        Mono<String> result = cacheService.getCached(testKey, String.class);
        StepVerifier.create(result)
            .verifyComplete();
    }
    
    @Test
    @DisplayName("Should generate cache key")
    void testGenerateCacheKey() {
        // When
        String key = cacheService.generateKey("stripe", "status", "pay_123");
        
        // Then
        assertThat(key).isEqualTo("connector:stripe:status:pay_123");
    }
    
    @Test
    @DisplayName("Should invalidate all cache for connector")
    void testInvalidateConnector() {
        // Given
        cacheService.put("connector:stripe:status:pay_1", "value1");
        cacheService.put("connector:stripe:status:pay_2", "value2");
        cacheService.put("connector:paypal:status:pay_3", "value3");
        
        // When
        cacheService.invalidateConnector("stripe");
        
        // Then
        Mono<String> result1 = cacheService.getCached("connector:stripe:status:pay_1", String.class);
        Mono<String> result2 = cacheService.getCached("connector:stripe:status:pay_2", String.class);
        Mono<String> result3 = cacheService.getCached("connector:paypal:status:pay_3", String.class);
        
        StepVerifier.create(result1).verifyComplete();
        StepVerifier.create(result2).verifyComplete();
        StepVerifier.create(result3)
            .assertNext(value -> assertThat(value).isEqualTo("value3"))
            .verifyComplete();
    }
}

