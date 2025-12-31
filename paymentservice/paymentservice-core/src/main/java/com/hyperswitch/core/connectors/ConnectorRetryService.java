package com.hyperswitch.core.connectors;

import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.function.Function;

/**
 * Service for handling retry logic for connector API calls
 * Implements exponential backoff retry strategy similar to hyperswitch
 */
@Service
public class ConnectorRetryService {
    
    private static final Logger log = LoggerFactory.getLogger(ConnectorRetryService.class);
    
    private static final int MAX_RETRIES = 3;
    private static final Duration INITIAL_DELAY = Duration.ofMillis(500);
    private static final Duration MAX_DELAY = Duration.ofSeconds(5);
    
    /**
     * Execute a connector API call with retry logic
     */
    public <T> Mono<Result<T, PaymentError>> executeWithRetry(
            Function<Void, Mono<Result<T, PaymentError>>> apiCall,
            String connectorName,
            String operation) {
        
        // Use Mono.defer to ensure the function is called on each retry attempt
        return Mono.defer(() -> apiCall.apply(null))
            .retryWhen(Retry.backoff(MAX_RETRIES, INITIAL_DELAY)
                .maxBackoff(MAX_DELAY)
                .filter(error -> shouldRetry(error))
                .doBeforeRetry(retrySignal -> {
                    log.warn("Retrying connector API call - Connector: {}, Operation: {}, Attempt: {}",
                        connectorName, operation, retrySignal.totalRetries() + 1);
                })
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    log.error("Connector API call failed after {} retries - Connector: {}, Operation: {}",
                        MAX_RETRIES, connectorName, operation);
                    return retrySignal.failure();
                }))
            .onErrorResume(error -> {
                log.error("Connector API call failed after retries: {}", error.getMessage(), error);
                return Mono.just(Result.err(PaymentError.of("CONNECTOR_API_CALL_FAILED",
                    "Failed after " + MAX_RETRIES + " retries: " + error.getMessage())));
            });
    }
    
    /**
     * Determine if an error should trigger a retry
     */
    private boolean shouldRetry(Throwable error) {
        if (error == null) {
            return false;
        }
        
        String errorMessage = error.getMessage();
        if (errorMessage == null) {
            return false;
        }
        
        // Retry on network errors, timeouts, and 5xx server errors
        return errorMessage.contains("timeout") ||
               errorMessage.contains("connection") ||
               errorMessage.contains("network") ||
               errorMessage.contains("500") ||
               errorMessage.contains("502") ||
               errorMessage.contains("503") ||
               errorMessage.contains("504");
    }
    
    /**
     * Execute with custom retry configuration
     */
    public <T> Mono<Result<T, PaymentError>> executeWithCustomRetry(
            Function<Void, Mono<Result<T, PaymentError>>> apiCall,
            int maxRetries,
            Duration initialDelay,
            Duration maxDelay,
            String connectorName,
            String operation) {
        
        return apiCall.apply(null)
            .retryWhen(Retry.backoff(maxRetries, initialDelay)
                .maxBackoff(maxDelay)
                .filter(this::shouldRetry)
                .doBeforeRetry(retrySignal -> {
                    log.warn("Retrying connector API call - Connector: {}, Operation: {}, Attempt: {}",
                        connectorName, operation, retrySignal.totalRetries() + 1);
                }))
            .onErrorResume(error -> {
                log.error("Connector API call failed after retries: {}", error.getMessage(), error);
                return Mono.just(Result.err(PaymentError.of("CONNECTOR_API_CALL_FAILED",
                    "Failed after " + maxRetries + " retries: " + error.getMessage())));
            });
    }
}

