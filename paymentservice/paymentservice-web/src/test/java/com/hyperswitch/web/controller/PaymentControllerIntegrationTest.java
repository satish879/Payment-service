package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.CreatePaymentRequest;
import com.hyperswitch.common.types.Amount;
import com.hyperswitch.core.payments.PaymentService;
import com.hyperswitch.core.test.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration tests for PaymentController
 */
@WebFluxTest(PaymentController.class)
@DisplayName("PaymentController Integration Tests")
class PaymentControllerIntegrationTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @MockBean
    private PaymentService paymentService;
    
    private String testMerchantId;
    
    @BeforeEach
    void setUp() {
        testMerchantId = TestUtils.generateTestMerchantId();
    }
    
    @Test
    @DisplayName("Should create payment via API")
    void testCreatePayment() {
        // Given
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setMerchantId(testMerchantId);
        request.setAmount(Amount.of(1000L, "USD"));
        request.setCustomerId("test_customer");
        
        when(paymentService.createPayment(any(CreatePaymentRequest.class)))
            .thenReturn(Mono.just(com.hyperswitch.common.types.Result.ok(
                com.hyperswitch.core.payments.PaymentIntent.builder()
                    .paymentId(com.hyperswitch.common.types.PaymentId.generate())
                    .merchantId(testMerchantId)
                    .amount(Amount.of(1000L, "USD"))
                    .status(com.hyperswitch.common.enums.PaymentStatus.REQUIRES_CONFIRMATION)
                    .build()
            )));
        
        // When & Then
        webTestClient.post()
            .uri("/api/payments/create")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.paymentId").exists()
            .jsonPath("$.status").isEqualTo("requires_confirmation");
    }
    
    @Test
    @DisplayName("Should handle invalid payment request")
    void testCreatePayment_InvalidRequest() {
        // Given
        CreatePaymentRequest request = new CreatePaymentRequest();
        // Missing required fields
        
        // When & Then
        webTestClient.post()
            .uri("/api/payments/create")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest();
    }
}

