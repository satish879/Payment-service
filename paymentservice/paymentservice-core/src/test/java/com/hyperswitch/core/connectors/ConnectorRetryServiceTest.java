package com.hyperswitch.core.connectors;

import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ConnectorRetryService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorRetryService Unit Tests")
class ConnectorRetryServiceTest {
    
    @InjectMocks
    private ConnectorRetryService retryService;
    
    private int attemptCount;
    
    @BeforeEach
    void setUp() {
        attemptCount = 0;
    }
    
    @Test
    @DisplayName("Should retry on network error")
    void testRetryOnNetworkError() {
        // Given
        Function<Void, Mono<Result<String, PaymentError>>> apiCall = unused -> {
            attemptCount++;
            if (attemptCount < 3) {
                return Mono.error(new RuntimeException("timeout"));
            }
            return Mono.just(Result.ok("success"));
        };
        
        // When
        Mono<Result<String, PaymentError>> result = retryService.executeWithRetry(
            apiCall, "stripe", "test");
        
        // Then
        StepVerifier.create(result)
            .assertNext(resultValue -> {
                assertThat(resultValue.isOk()).isTrue();
                assertThat(attemptCount).isEqualTo(3);
            })
            .verifyComplete();
    }
    
    @Test
    @DisplayName("Should fail after max retries")
    void testFailAfterMaxRetries() {
        // Given
        Function<Void, Mono<Result<String, PaymentError>>> apiCall = unused -> {
            attemptCount++;
            return Mono.error(new RuntimeException("timeout"));
        };
        
        // When
        Mono<Result<String, PaymentError>> result = retryService.executeWithRetry(
            apiCall, "stripe", "test");
        
        // Then
        StepVerifier.create(result)
            .assertNext(resultValue -> {
                assertThat(resultValue.isErr()).isTrue();
                assertThat(attemptCount).isGreaterThanOrEqualTo(3);
            })
            .verifyComplete();
    }
    
    @Test
    @DisplayName("Should not retry on non-retryable error")
    void testNoRetryOnNonRetryableError() {
        // Given
        Function<Void, Mono<Result<String, PaymentError>>> apiCall = unused -> {
            attemptCount++;
            return Mono.just(Result.err(PaymentError.of("INVALID_REQUEST", "Bad request")));
        };
        
        // When
        Mono<Result<String, PaymentError>> result = retryService.executeWithRetry(
            apiCall, "stripe", "test");
        
        // Then
        StepVerifier.create(result)
            .assertNext(resultValue -> {
                assertThat(resultValue.isErr()).isTrue();
                assertThat(attemptCount).isEqualTo(1); // Should not retry
            })
            .verifyComplete();
    }
}

