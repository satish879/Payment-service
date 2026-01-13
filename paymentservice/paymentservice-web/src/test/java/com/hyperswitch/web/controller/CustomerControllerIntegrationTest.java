package com.hyperswitch.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperswitch.common.dto.CustomerRequest;
import com.hyperswitch.common.dto.CustomerResponse;
import com.hyperswitch.common.types.CustomerId;
import com.hyperswitch.common.types.MerchantId;
import com.hyperswitch.common.types.Result;
import com.hyperswitch.core.customers.CustomerService;
import com.hyperswitch.common.errors.PaymentError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration tests for CustomerController
 * Tests all customer management endpoints from High Priority Endpoints Postman collection
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
@DisplayName("CustomerController Integration Tests")
class CustomerControllerIntegrationTest {

    @Autowired
    private ApplicationContext context;

    private WebTestClient webTestClient;

    private CustomerService customerService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @org.springframework.context.annotation.Primary
        public CustomerService customerService() {
            return org.mockito.Mockito.mock(CustomerService.class);
        }
    }

    @Autowired
    private ObjectMapper objectMapper;

    private String testMerchantId;
    private String testCustomerId;

    @BeforeEach
    void setUp() {
        this.webTestClient = WebTestClient.bindToApplicationContext(context).build();
        this.customerService = context.getBean("customerService", CustomerService.class);
        testMerchantId = "merchant_123";
        testCustomerId = "cust_" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    @DisplayName("POST /api/customers - Should create customer successfully")
    void testCreateCustomer_Success() throws Exception {
        // Given - Use JSON string matching Postman collection format (camelCase, String values)
        String requestJson = String.format(
            "{\"merchantId\":\"%s\",\"name\":\"John Doe\",\"email\":\"john.doe@example.com\",\"phone\":\"+1234567890\",\"metadata\":{}}",
            testMerchantId
        );

        CustomerResponse response = CustomerResponse.builder()
            .customerId(CustomerId.of(testCustomerId))
            .merchantId(MerchantId.of(testMerchantId))
            .name("John Doe")
            .email("john.doe@example.com")
            .phone("+1234567890")
            .metadata(Map.of())
            .build();

        when(customerService.createCustomer(any(CustomerRequest.class)))
            .thenReturn(Mono.just(Result.ok(response)));

        // When & Then
        webTestClient.post()
            .uri("/api/customers")
            .header("X-Merchant-Id", testMerchantId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestJson)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.customerId").isEqualTo(testCustomerId)
            .jsonPath("$.name").isEqualTo("John Doe")
            .jsonPath("$.email").isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("GET /api/customers/{id} - Should get customer successfully")
    void testGetCustomer_Success() {
        // Given
        CustomerResponse response = CustomerResponse.builder()
            .customerId(CustomerId.of(testCustomerId))
            .merchantId(MerchantId.of(testMerchantId))
            .name("John Doe")
            .email("john.doe@example.com")
            .build();

        when(customerService.getCustomer(any(CustomerId.class)))
            .thenReturn(Mono.just(Result.ok(response)));

        // When & Then
        webTestClient.get()
            .uri("/api/customers/{id}", testCustomerId)
            .header("X-Merchant-Id", testMerchantId)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.customerId").isEqualTo(testCustomerId)
            .jsonPath("$.name").isEqualTo("John Doe");
    }

    @Test
    @DisplayName("POST /api/customers/{id} - Should update customer successfully")
    void testUpdateCustomer_Success() throws Exception {
        // Given - Use JSON string matching Postman collection format
        String requestJson = String.format(
            "{\"merchantId\":\"%s\",\"name\":\"Jane Doe\",\"email\":\"jane.doe@example.com\",\"phone\":\"+1234567890\",\"metadata\":{\"preferences\":\"email\"}}",
            testMerchantId
        );

        CustomerResponse response = CustomerResponse.builder()
            .customerId(CustomerId.of(testCustomerId))
            .merchantId(MerchantId.of(testMerchantId))
            .name("Jane Doe")
            .email("jane.doe@example.com")
            .build();

        when(customerService.updateCustomer(any(CustomerId.class), any(CustomerRequest.class)))
            .thenReturn(Mono.just(Result.ok(response)));

        // When & Then
        webTestClient.post()
            .uri("/api/customers/{id}", testCustomerId)
            .header("X-Merchant-Id", testMerchantId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestJson)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.name").isEqualTo("Jane Doe");
    }

    @Test
    @DisplayName("DELETE /api/customers/{id} - Should delete customer successfully")
    void testDeleteCustomer_Success() {
        // Given
        when(customerService.deleteCustomer(any(CustomerId.class)))
            .thenReturn(Mono.just(Result.ok(null)));

        // When & Then
        webTestClient.delete()
            .uri("/api/customers/{id}", testCustomerId)
            .header("X-Merchant-Id", testMerchantId)
            .exchange()
            .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("GET /api/customers - Should list customers successfully")
    void testListCustomers_Success() {
        // Given
        CustomerResponse customer1 = CustomerResponse.builder()
            .customerId(CustomerId.of("cust_1"))
            .merchantId(MerchantId.of(testMerchantId))
            .name("Customer 1")
            .build();

        CustomerResponse customer2 = CustomerResponse.builder()
            .customerId(CustomerId.of("cust_2"))
            .merchantId(MerchantId.of(testMerchantId))
            .name("Customer 2")
            .build();

        when(customerService.listCustomers(any(MerchantId.class), any(Pageable.class)))
            .thenReturn(Mono.just(Result.ok(Flux.just(customer1, customer2))));

        // When & Then
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/customers")
                .queryParam("merchant_id", testMerchantId)
                .queryParam("page", "0")
                .queryParam("size", "10")
                .build())
            .header("X-Merchant-Id", testMerchantId)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$[0].customerId").exists()
            .jsonPath("$[1].customerId").exists();
    }
}

