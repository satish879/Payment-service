package com.hyperswitch.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperswitch.common.dto.*;
import com.hyperswitch.common.types.Amount;
import com.hyperswitch.common.types.PaymentId;
import com.hyperswitch.core.payments.PaymentService;
import com.hyperswitch.core.payments.Refund;
import com.hyperswitch.common.types.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for RefundController
 * Tests all refund endpoints from High Priority Endpoints Postman collection
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
@DisplayName("RefundController Integration Tests")
class RefundControllerIntegrationTest {

    @Autowired
    private ApplicationContext context;

    private WebTestClient webTestClient;

    private PaymentService paymentService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @org.springframework.context.annotation.Primary
        public PaymentService paymentService() {
            return org.mockito.Mockito.mock(PaymentService.class);
        }
    }

    @Autowired
    private ObjectMapper objectMapper;

    private String testMerchantId;
    private String testPaymentId;
    private String testRefundId;

    @BeforeEach
    void setUp() {
        this.webTestClient = WebTestClient.bindToApplicationContext(context).build();
        this.paymentService = context.getBean("paymentService", PaymentService.class);
        testMerchantId = "merchant_123";
        testPaymentId = "pay_" + UUID.randomUUID().toString().substring(0, 8);
        testRefundId = "ref_" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    @DisplayName("POST /api/v2/refunds - Should create refund successfully")
    void testCreateRefund_Success() throws Exception {
        // Given - Use JSON string matching Postman collection format
        // Note: Postman collection shows /api/payments/{payment_id}/refund, but this test uses /api/v2/refunds
        String requestJson = String.format(
            "{\"paymentId\":\"%s\",\"amount\":{\"value\":500,\"currencyCode\":\"USD\"},\"reason\":\"Customer requested refund\",\"metadata\":{}}",
            testPaymentId
        );

        Refund refund = Refund.builder()
            .refundId(testRefundId)
            .paymentId(testPaymentId)
            .amount(Amount.of(BigDecimal.valueOf(500), "USD"))
            .status("pending")
            .reason("Customer requested refund")
            .createdAt(java.time.Instant.now())
            .build();

        when(paymentService.createRefundV2(anyString(), any(RefundsCreateRequest.class)))
            .thenReturn(Mono.just(Result.ok(refund)));

        // When & Then
        webTestClient.post()
            .uri("/api/v2/refunds")
            .header("X-Merchant-Id", testMerchantId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestJson)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.refundId").isEqualTo(testRefundId)
            .jsonPath("$.amount.value").isEqualTo(500)
            .jsonPath("$.status").isEqualTo("pending");
    }

    @Test
    @DisplayName("GET /api/v2/refunds/{id} - Should get refund successfully")
    void testGetRefund_Success() {
        // Given
        Refund refund = Refund.builder()
            .refundId(testRefundId)
            .paymentId(testPaymentId)
            .amount(Amount.of(BigDecimal.valueOf(500), "USD"))
            .status("succeeded")
            .createdAt(java.time.Instant.now())
            .build();

        when(paymentService.retrieveRefundV2(anyString(), anyString(), any(RefundsRetrieveRequest.class)))
            .thenReturn(Mono.just(Result.ok(refund)));

        // When & Then
        webTestClient.get()
            .uri("/api/v2/refunds/{id}", testRefundId)
            .header("X-Merchant-Id", testMerchantId)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.refundId").isEqualTo(testRefundId)
            .jsonPath("$.status").isEqualTo("succeeded");
    }

    @Test
    @DisplayName("POST /api/v2/refunds/list - Should list refunds successfully")
    void testListRefunds_Success() throws Exception {
        // Given
        RefundListFilterConstraints constraints = new RefundListFilterConstraints();
        constraints.setLimit(10);
        constraints.setOffset(0);
        constraints.setStatus("SUCCEEDED");
        constraints.setConnector("stripe");
        constraints.setCurrency("USD");

        Refund refund1 = Refund.builder()
            .refundId("ref_1")
            .paymentId(testPaymentId)
            .amount(Amount.of(BigDecimal.valueOf(500), "USD"))
            .status("succeeded")
            .createdAt(java.time.Instant.now())
            .build();

        Refund refund2 = Refund.builder()
            .refundId("ref_2")
            .paymentId(testPaymentId)
            .amount(Amount.of(BigDecimal.valueOf(300), "USD"))
            .status("succeeded")
            .createdAt(java.time.Instant.now())
            .build();

        RefundListResponse response = new RefundListResponse();
        response.setRefunds(java.util.List.of(
            java.util.Map.of("refundId", refund1.getRefundId(), "status", refund1.getStatus()),
            java.util.Map.of("refundId", refund2.getRefundId(), "status", refund2.getStatus())
        ));
        response.setTotal(2L);

        when(paymentService.listRefundsV2(anyString(), any(RefundListFilterConstraints.class)))
            .thenReturn(Mono.just(Result.ok(response)));

        // When & Then
        webTestClient.post()
            .uri("/api/v2/refunds/list")
            .header("X-Merchant-Id", testMerchantId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(constraints))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.refunds[0].refundId").exists()
            .jsonPath("$.total").isEqualTo(2);
    }

    @Test
    @DisplayName("PUT /api/v2/refunds/{id}/update-metadata - Should update refund metadata successfully")
    void testUpdateRefundMetadata_Success() throws Exception {
        // Given
        RefundMetadataUpdateRequest request = new RefundMetadataUpdateRequest();
        request.setReason("Updated reason");
        request.setMetadata(Map.of("updated", "true"));

        Refund updatedRefund = Refund.builder()
            .refundId(testRefundId)
            .paymentId(testPaymentId)
            .amount(Amount.of(BigDecimal.valueOf(500), "USD"))
            .status("succeeded")
            .reason("Updated reason")
            .createdAt(java.time.Instant.now())
            .build();

        when(paymentService.updateRefundMetadataV2(anyString(), anyString(), any(RefundMetadataUpdateRequest.class)))
            .thenReturn(Mono.just(Result.ok(updatedRefund)));

        // When & Then
        webTestClient.put()
            .uri("/api/v2/refunds/{id}/update-metadata", testRefundId)
            .header("X-Merchant-Id", testMerchantId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(request))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.reason").isEqualTo("Updated reason");
    }
}

