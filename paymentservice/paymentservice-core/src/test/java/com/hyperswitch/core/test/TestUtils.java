package com.hyperswitch.core.test;

import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Test utilities for reactive testing
 */
public class TestUtils {
    
    private TestUtils() {
        // Utility class
    }
    
    /**
     * Verify a successful Result
     */
    public static <T> void verifySuccess(Mono<Result<T, PaymentError>> resultMono, T expectedValue) {
        StepVerifier.create(resultMono)
            .expectNextMatches(result -> {
                if (result.isOk()) {
                    T value = result.unwrap();
                    return value != null && (expectedValue == null || value.equals(expectedValue));
                }
                return false;
            })
            .verifyComplete();
    }
    
    /**
     * Verify an error Result
     */
    public static void verifyError(Mono<Result<?, PaymentError>> resultMono, String expectedErrorCode) {
        StepVerifier.create(resultMono)
            .expectNextMatches(result -> {
                if (result.isErr()) {
                    PaymentError error = result.unwrapErr();
                    return error.getCode().equals(expectedErrorCode);
                }
                return false;
            })
            .verifyComplete();
    }
    
    /**
     * Verify a successful Result with custom matcher
     */
    public static <T> void verifySuccessWithMatcher(
            Mono<Result<T, PaymentError>> resultMono,
            java.util.function.Predicate<T> matcher) {
        StepVerifier.create(resultMono)
            .expectNextMatches(result -> {
                if (result.isOk()) {
                    return matcher.test(result.unwrap());
                }
                return false;
            })
            .verifyComplete();
    }
    
    /**
     * Create a test timeout
     */
    public static Duration testTimeout() {
        return Duration.ofSeconds(5);
    }
    
    /**
     * Wait for async operation
     */
    public static void waitForAsync() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Generate test ID
     */
    public static String generateTestId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Generate test merchant ID
     */
    public static String generateTestMerchantId() {
        return "merchant_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Generate test payment ID
     */
    public static String generateTestPaymentId() {
        return "pay_" + UUID.randomUUID().toString().substring(0, 8);
    }
}

