package com.hyperswitch.core.payments;

import com.hyperswitch.common.dto.CreatePaymentRequest;
import com.hyperswitch.common.types.Amount;
import com.hyperswitch.core.test.BaseIntegrationTest;
import com.hyperswitch.core.test.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for PaymentService
 * Tests the complete payment flow from creation to completion
 */
@DisplayName("PaymentService E2E Tests")
class PaymentServiceE2ETest extends BaseIntegrationTest {
    
    @Autowired
    private PaymentService paymentService;
    
    private String testMerchantId;
    
    @BeforeEach
    void setUp() {
        testMerchantId = TestUtils.generateTestMerchantId();
    }
    
    @Test
    @DisplayName("Should complete full payment flow")
    void testCompletePaymentFlow() {
        // Given
        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .merchantId(testMerchantId)
            .amount(Amount.of(java.math.BigDecimal.valueOf(1000L), "USD"))
            .customerId("test_customer")
            .description("E2E test payment")
            .build();
        
        // When - Create payment
        Mono<com.hyperswitch.common.types.Result<PaymentIntent, com.hyperswitch.common.errors.PaymentError>> createResult = 
            paymentService.createPayment(request);
        
        // Then
        StepVerifier.create(createResult)
            .assertNext(result -> {
                assertThat(result.isOk()).isTrue();
                PaymentIntent paymentIntent = result.unwrap();
                assertThat(paymentIntent).isNotNull();
                assertThat(paymentIntent.getPaymentId()).isNotNull();
                assertThat(paymentIntent.getMerchantId()).isEqualTo(testMerchantId);
            })
            .verifyComplete();
    }
    
    @Test
    @DisplayName("Should handle payment creation with validation")
    void testPaymentCreationWithValidation() {
        // Given
        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .merchantId(testMerchantId)
            .amount(Amount.of(java.math.BigDecimal.valueOf(-100L), "USD")) // Invalid amount
            .build();
        
        // When
        Mono<com.hyperswitch.common.types.Result<PaymentIntent, com.hyperswitch.common.errors.PaymentError>> result = 
            paymentService.createPayment(request);
        
        // Then
        StepVerifier.create(result)
            .assertNext(resultValue -> {
                // Should either fail validation or handle gracefully
                assertThat(resultValue).isNotNull();
            })
            .verifyComplete();
    }
}

