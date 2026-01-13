package com.hyperswitch.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperswitch.common.dto.*;
import com.hyperswitch.common.types.Amount;
import com.hyperswitch.common.types.PaymentId;
import com.hyperswitch.core.payments.PaymentIntent;
import com.hyperswitch.core.payments.PaymentService;
import com.hyperswitch.core.payments.Refund;
import com.hyperswitch.core.payments.RefundRequest;
import com.hyperswitch.core.payments.ConfirmPaymentRequest;
import com.hyperswitch.core.payments.CapturePaymentRequest;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
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
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Comprehensive integration tests for PaymentController
 * Tests all endpoints from High Priority Endpoints Postman collection
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
@Import(TestServiceConfiguration.class)
@DisplayName("PaymentController Comprehensive Integration Tests")
class PaymentControllerComprehensiveIntegrationTest {

    @Autowired
    private ApplicationContext context;

    private WebTestClient webTestClient;

    private PaymentService paymentService;


    @Autowired
    private ObjectMapper objectMapper;

    private String testMerchantId;
    private String testPaymentId;
    private String testCustomerId;

    @BeforeEach
    void setUp() {
        this.webTestClient = WebTestClient.bindToApplicationContext(context).build();
        this.paymentService = context.getBean("paymentService", PaymentService.class);
        testMerchantId = "merchant_123";
        testPaymentId = "pay_" + UUID.randomUUID().toString().substring(0, 8);
        testCustomerId = "cust_123";
    }

    // ========== CREATE PAYMENT TESTS ==========

    @Test
    @DisplayName("POST /api/payments - Should create payment successfully")
    void testCreatePayment_Success() throws Exception {
        // Given - Use JSON string matching Postman collection format (camelCase, String values)
        String requestJson = String.format(
            "{\"amount\":{\"value\":1000,\"currencyCode\":\"USD\"},\"merchantId\":\"%s\",\"paymentMethod\":\"CARD\",\"customerId\":\"%s\",\"description\":\"Payment for order #12345\",\"captureMethod\":\"AUTOMATIC\",\"confirm\":false,\"metadata\":{}}",
            testMerchantId, testCustomerId
        );

        PaymentIntent mockPaymentIntent = PaymentIntent.builder()
            .paymentId(PaymentId.of(testPaymentId))
            .merchantId(testMerchantId)
            .amount(Amount.of(BigDecimal.valueOf(1000), "USD"))
            .status(com.hyperswitch.common.enums.PaymentStatus.REQUIRES_CONFIRMATION)
            .paymentMethod(com.hyperswitch.common.enums.PaymentMethod.CARD)
            .customerId(testCustomerId)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .metadata(Map.of("payment_method", "CARD"))
            .build();

        when(paymentService.createPayment(any(CreatePaymentRequest.class)))
            .thenReturn(Mono.just(Result.ok(mockPaymentIntent)));

        // When & Then
        webTestClient.post()
            .uri("/api/payments")
            .header("X-Merchant-Id", testMerchantId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestJson)
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.paymentId.value").isEqualTo(testPaymentId)
            .jsonPath("$.merchantId").isEqualTo(testMerchantId)
            .jsonPath("$.amount.value").isEqualTo(1000)
            .jsonPath("$.status").isEqualTo("REQUIRES_CONFIRMATION")
            .jsonPath("$.paymentMethod").isEqualTo("CARD");
    }

    @Test
    @DisplayName("POST /api/payments - Should return 400 when amount is null")
    void testCreatePayment_AmountNull_BadRequest() throws Exception {
        // Given
        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .merchantId(testMerchantId)
            .amount(null)
            .build();

        when(paymentService.createPayment(any(CreatePaymentRequest.class)))
            .thenReturn(Mono.just(Result.err(PaymentError.of("INVALID_REQUEST", "Amount is required"))));

        // When & Then
        webTestClient.post()
            .uri("/api/payments")
            .header("X-Merchant-Id", testMerchantId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(request))
            .exchange()
            .expectStatus().isBadRequest();
    }

    // ========== CONFIRM PAYMENT TESTS ==========

    @Test
    @DisplayName("POST /api/payments/{id}/confirm - Should confirm payment successfully")
    void testConfirmPayment_Success() throws Exception {
        // Given - Use JSON string matching Postman collection format
        String requestJson = "{\"paymentMethodId\":\"pm_123\",\"returnUrl\":\"https://example.com/return\"}";

        PaymentIntent confirmedIntent = PaymentIntent.builder()
            .paymentId(PaymentId.of(testPaymentId))
            .merchantId(testMerchantId)
            .status(com.hyperswitch.common.enums.PaymentStatus.PROCESSING)
            .build();

        when(paymentService.confirmPayment(any(PaymentId.class), any(ConfirmPaymentRequest.class)))
            .thenReturn(Mono.just(Result.ok(confirmedIntent)));

        // When & Then
        webTestClient.post()
            .uri("/api/payments/{paymentId}/confirm", testPaymentId)
            .header("X-Merchant-Id", testMerchantId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestJson)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("PROCESSING");
    }

    // ========== CAPTURE PAYMENT TESTS ==========

    @Test
    @DisplayName("POST /api/payments/{id}/capture - Should capture payment successfully")
    void testCapturePayment_Success() throws Exception {
        // Given - Use JSON string matching Postman collection format
        String requestJson = "{\"amount\":{\"value\":1000,\"currencyCode\":\"USD\"}}";

        PaymentIntent capturedIntent = PaymentIntent.builder()
            .paymentId(PaymentId.of(testPaymentId))
            .merchantId(testMerchantId)
            .status(com.hyperswitch.common.enums.PaymentStatus.SUCCEEDED)
            .build();

        when(paymentService.capturePayment(any(PaymentId.class), any(CapturePaymentRequest.class)))
            .thenReturn(Mono.just(Result.ok(capturedIntent)));

        // When & Then
        webTestClient.post()
            .uri("/api/payments/{paymentId}/capture", testPaymentId)
            .header("X-Merchant-Id", testMerchantId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestJson)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("SUCCEEDED");
    }

    // ========== GET PAYMENT TESTS ==========

    @Test
    @DisplayName("GET /api/payments/{id} - Should get payment successfully")
    void testGetPayment_Success() {
        // Given
        PaymentIntent paymentIntent = PaymentIntent.builder()
            .paymentId(PaymentId.of(testPaymentId))
            .merchantId(testMerchantId)
            .amount(Amount.of(BigDecimal.valueOf(1000), "USD"))
            .status(com.hyperswitch.common.enums.PaymentStatus.REQUIRES_CONFIRMATION)
            .build();

        when(paymentService.getPayment(any(PaymentId.class)))
            .thenReturn(Mono.just(Result.ok(paymentIntent)));

        // When & Then
        webTestClient.get()
            .uri("/api/payments/{paymentId}", testPaymentId)
            .header("X-Merchant-Id", testMerchantId)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.paymentId.value").isEqualTo(testPaymentId)
            .jsonPath("$.merchantId").isEqualTo(testMerchantId);
    }

    @Test
    @DisplayName("GET /api/payments/{id} - Should return 404 when payment not found")
    void testGetPayment_NotFound() {
        // Given
        when(paymentService.getPayment(any(PaymentId.class)))
            .thenReturn(Mono.just(Result.err(PaymentError.of("PAYMENT_NOT_FOUND", "Payment not found"))));

        // When & Then
        webTestClient.get()
            .uri("/api/payments/{paymentId}", testPaymentId)
            .header("X-Merchant-Id", testMerchantId)
            .exchange()
            .expectStatus().isNotFound();
    }

    // ========== UPDATE PAYMENT TESTS ==========

    @Test
    @DisplayName("POST /api/payments/{id} - Should update payment successfully")
    void testUpdatePayment_Success() throws Exception {
        // Given - Use JSON string matching Postman collection format
        String requestJson = "{\"amount\":{\"value\":1500,\"currencyCode\":\"USD\"},\"description\":\"Updated payment description\",\"metadata\":{\"order_id\":\"order_123\"}}";

        PaymentIntent updatedIntent = PaymentIntent.builder()
            .paymentId(PaymentId.of(testPaymentId))
            .merchantId(testMerchantId)
            .amount(Amount.of(BigDecimal.valueOf(1500), "USD"))
            .status(com.hyperswitch.common.enums.PaymentStatus.REQUIRES_CONFIRMATION)
            .build();

        when(paymentService.updatePayment(any(PaymentId.class), any(UpdatePaymentRequest.class)))
            .thenReturn(Mono.just(Result.ok(updatedIntent)));

        // When & Then
        webTestClient.post()
            .uri("/api/payments/{paymentId}", testPaymentId)
            .header("X-Merchant-Id", testMerchantId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestJson)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.amount.value").isEqualTo(1500);
    }

    // ========== CANCEL PAYMENT TESTS ==========

    @Test
    @DisplayName("POST /api/payments/{id}/cancel - Should cancel payment successfully")
    void testCancelPayment_Success() throws Exception {
        // Given - Use JSON string matching Postman collection format
        String requestJson = "{\"cancellationReason\":\"Customer requested cancellation\"}";

        PaymentIntent cancelledIntent = PaymentIntent.builder()
            .paymentId(PaymentId.of(testPaymentId))
            .merchantId(testMerchantId)
            .status(com.hyperswitch.common.enums.PaymentStatus.CANCELLED)
            .build();

        when(paymentService.cancelPayment(any(PaymentId.class), any(CancelPaymentRequest.class)))
            .thenReturn(Mono.just(Result.ok(cancelledIntent)));

        // When & Then
        webTestClient.post()
            .uri("/api/payments/{paymentId}/cancel", testPaymentId)
            .header("X-Merchant-Id", testMerchantId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestJson)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("CANCELLED");
    }

    // ========== GET CLIENT SECRET TESTS ==========

    @Test
    @DisplayName("GET /api/payments/{id}/client_secret - Should get client secret successfully")
    void testGetClientSecret_Success() {
        // Given
        String clientSecret = "pi_" + UUID.randomUUID().toString();
        when(paymentService.getClientSecret(any(PaymentId.class)))
            .thenReturn(Mono.just(Result.ok(clientSecret)));

        // When & Then
        webTestClient.get()
            .uri("/api/payments/{paymentId}/client_secret", testPaymentId)
            .header("X-Merchant-Id", testMerchantId)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.client_secret").isEqualTo(clientSecret);
    }

    // ========== REFUND TESTS ==========

    @Test
    @DisplayName("POST /api/payments/{id}/refund - Should create refund successfully")
    void testRefundPayment_Success() throws Exception {
        // Given - Use JSON string matching Postman collection format
        String requestJson = "{\"amount\":{\"value\":500,\"currencyCode\":\"USD\"},\"reason\":\"Customer requested refund\",\"metadata\":{}}";

        Refund refund = Refund.builder()
            .refundId("ref_" + UUID.randomUUID().toString().substring(0, 8))
            .paymentId(testPaymentId)
            .amount(Amount.of(BigDecimal.valueOf(500), "USD"))
            .status("pending")
            .reason("Customer requested refund")
            .createdAt(java.time.Instant.now())
            .build();

        when(paymentService.refundPayment(any(PaymentId.class), any(RefundRequest.class)))
            .thenReturn(Mono.just(Result.ok(refund)));

        // When & Then
        webTestClient.post()
            .uri("/api/payments/{paymentId}/refund", testPaymentId)
            .header("X-Merchant-Id", testMerchantId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestJson)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.refundId").exists()
            .jsonPath("$.amount.value").isEqualTo(500);
    }

    // ========== 3DS TESTS ==========

    @Test
    @DisplayName("POST /api/payments/{id}/3ds/challenge - Should handle 3DS challenge")
    void testHandle3DSChallenge_Success() throws Exception {
        // Given - Use JSON string matching Postman collection format
        String requestJson = "{\"returnUrl\":\"https://example.com/return\"}";

        ThreeDSResponse response = ThreeDSResponse.builder()
            .redirectUrl("https://3ds.example.com/challenge")
            .build();

        when(paymentService.handle3DSChallenge(any(PaymentId.class), any(ThreeDSRequest.class)))
            .thenReturn(Mono.just(Result.ok(response)));

        // When & Then
        webTestClient.post()
            .uri("/api/payments/{paymentId}/3ds/challenge", testPaymentId)
            .header("X-Merchant-Id", testMerchantId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestJson)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.redirectUrl").exists();
    }

    @Test
    @DisplayName("POST /api/payments/{id}/3ds/resume - Should resume payment after 3DS")
    void testResumePaymentAfter3DS_Success() throws Exception {
        // Given - Use JSON string matching Postman collection format
        String requestJson = "{\"authenticationId\":\"auth_123\"}";
        
        PaymentIntent resumedIntent = PaymentIntent.builder()
            .paymentId(PaymentId.of(testPaymentId))
            .merchantId(testMerchantId)
            .status(com.hyperswitch.common.enums.PaymentStatus.SUCCEEDED)
            .build();

        when(paymentService.resumePaymentAfter3DS(any(PaymentId.class), anyString()))
            .thenReturn(Mono.just(Result.ok(resumedIntent)));

        // When & Then
        webTestClient.post()
            .uri("/api/payments/{paymentId}/3ds/resume", testPaymentId)
            .header("X-Merchant-Id", testMerchantId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestJson)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("SUCCEEDED");
    }
}

