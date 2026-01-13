package com.hyperswitch.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperswitch.common.dto.PaymentMethodRequest;
import com.hyperswitch.common.dto.PaymentMethodResponse;
import com.hyperswitch.common.types.CustomerId;
import com.hyperswitch.common.types.MerchantId;
import com.hyperswitch.common.types.PaymentMethodId;
import com.hyperswitch.common.types.Result;
import com.hyperswitch.core.paymentmethods.PaymentMethodService;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.web.config.TestServiceConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for PaymentMethodController
 * Tests all payment method management endpoints from High Priority Endpoints Postman collection
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
@Import(TestServiceConfiguration.class)
@DisplayName("PaymentMethodController Integration Tests")
class PaymentMethodControllerIntegrationTest {

    @Autowired
    private ApplicationContext context;

    private WebTestClient webTestClient;

    private PaymentMethodService paymentMethodService;


    @Autowired
    private ObjectMapper objectMapper;

    private String testMerchantId;
    private String testCustomerId;
    private String testPaymentMethodId;

    @BeforeEach
    void setUp() {
        this.webTestClient = WebTestClient.bindToApplicationContext(context).build();
        this.paymentMethodService = context.getBean("paymentMethodService", PaymentMethodService.class);
        testMerchantId = "merchant_123";
        testCustomerId = "cust_123";
        testPaymentMethodId = "pm_" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    @DisplayName("POST /api/payment_methods - Should create payment method successfully")
    void testCreatePaymentMethod_Success() throws Exception {
        // Given - Use JSON string matching Postman collection format (camelCase, String values for IDs)
        String requestJson = String.format(
            "{\"customerId\":\"%s\",\"merchantId\":\"%s\",\"paymentMethodType\":\"CARD\",\"paymentMethodData\":{\"cardNumber\":\"4242424242424242\",\"expiryMonth\":12,\"expiryYear\":2025,\"cvc\":\"123\",\"cardholderName\":\"John Doe\"}}",
            testCustomerId, testMerchantId
        );

        PaymentMethodResponse response = PaymentMethodResponse.builder()
            .paymentMethodId(PaymentMethodId.of(testPaymentMethodId))
            .customerId(CustomerId.of(testCustomerId))
            .merchantId(MerchantId.of(testMerchantId))
            .paymentMethodType("CARD")
            .build();

        when(paymentMethodService.createPaymentMethod(any(PaymentMethodRequest.class)))
            .thenReturn(Mono.just(Result.ok(response)));

        // When & Then
        webTestClient.post()
            .uri("/api/payment_methods")
            .header("X-Merchant-Id", testMerchantId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestJson)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.paymentMethodId").isEqualTo(testPaymentMethodId)
            .jsonPath("$.paymentMethodType").isEqualTo("CARD");
    }

    @Test
    @DisplayName("GET /api/payment_methods/{id} - Should get payment method successfully")
    void testGetPaymentMethod_Success() {
        // Given
        PaymentMethodResponse response = PaymentMethodResponse.builder()
            .paymentMethodId(PaymentMethodId.of(testPaymentMethodId))
            .customerId(CustomerId.of(testCustomerId))
            .merchantId(MerchantId.of(testMerchantId))
            .paymentMethodType("CARD")
            .build();

        when(paymentMethodService.getPaymentMethod(any(PaymentMethodId.class)))
            .thenReturn(Mono.just(Result.ok(response)));

        // When & Then
        webTestClient.get()
            .uri("/api/payment_methods/{id}", testPaymentMethodId)
            .header("X-Merchant-Id", testMerchantId)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.paymentMethodId").isEqualTo(testPaymentMethodId);
    }

    @Test
    @DisplayName("GET /api/customers/{customerId}/payment_methods - Should list customer payment methods")
    void testListCustomerPaymentMethods_Success() {
        // Given
        PaymentMethodResponse pm1 = PaymentMethodResponse.builder()
            .paymentMethodId(PaymentMethodId.of("pm_1"))
            .customerId(CustomerId.of(testCustomerId))
            .paymentMethodType("CARD")
            .build();

        PaymentMethodResponse pm2 = PaymentMethodResponse.builder()
            .paymentMethodId(PaymentMethodId.of("pm_2"))
            .customerId(CustomerId.of(testCustomerId))
            .paymentMethodType("CARD")
            .build();

        when(paymentMethodService.listCustomerPaymentMethods(any(CustomerId.class)))
            .thenReturn(Mono.just(Result.ok(Flux.just(pm1, pm2))));

        // When & Then
        webTestClient.get()
            .uri("/api/customers/{customerId}/payment_methods", testCustomerId)
            .header("X-Merchant-Id", testMerchantId)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$[0].paymentMethodId").exists()
            .jsonPath("$[1].paymentMethodId").exists();
    }

    @Test
    @DisplayName("POST /api/customers/{customerId}/payment_methods/{pm_id}/default - Should set default payment method")
    void testSetDefaultPaymentMethod_Success() {
        // Given
        when(paymentMethodService.setDefaultPaymentMethod(any(CustomerId.class), any(PaymentMethodId.class)))
            .thenReturn(Mono.just(Result.ok(null)));

        // When & Then
        webTestClient.post()
            .uri("/api/customers/{customerId}/payment_methods/{paymentMethodId}/default",
                testCustomerId, testPaymentMethodId)
            .header("X-Merchant-Id", testMerchantId)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    @DisplayName("DELETE /api/payment_methods/{id} - Should delete payment method successfully")
    void testDeletePaymentMethod_Success() {
        // Given
        when(paymentMethodService.deletePaymentMethod(any(PaymentMethodId.class)))
            .thenReturn(Mono.just(Result.ok(null)));

        // When & Then
        webTestClient.delete()
            .uri("/api/payment_methods/{id}", testPaymentMethodId)
            .header("X-Merchant-Id", testMerchantId)
            .exchange()
            .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("GET /api/payment_methods/client_secret - Should get payment method by client secret")
    void testGetPaymentMethodByClientSecret_Success() {
        // Given
        String clientSecret = "pi_" + UUID.randomUUID().toString();
        PaymentMethodResponse response = PaymentMethodResponse.builder()
            .paymentMethodId(PaymentMethodId.of(testPaymentMethodId))
            .paymentMethodType("CARD")
            .build();

        when(paymentMethodService.getPaymentMethodByClientSecret(anyString()))
            .thenReturn(Mono.just(Result.ok(response)));

        // When & Then
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/payment_methods/client_secret")
                .queryParam("client_secret", clientSecret)
                .build())
            .header("X-Merchant-Id", testMerchantId)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.paymentMethodId").isEqualTo(testPaymentMethodId);
    }
}

