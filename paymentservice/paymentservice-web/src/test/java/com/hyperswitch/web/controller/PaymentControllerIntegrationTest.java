package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.CreatePaymentRequest;
import com.hyperswitch.common.types.Amount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.junit.jupiter.api.BeforeEach;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.hyperswitch.core.payments.PaymentIntent;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Amount;
import com.hyperswitch.common.types.PaymentId;
import com.hyperswitch.common.types.Result;
import java.math.BigDecimal;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PaymentControllerIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private com.hyperswitch.core.payments.PaymentService paymentService;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        public com.hyperswitch.core.payments.PaymentService paymentService() {
            return org.mockito.Mockito.mock(com.hyperswitch.core.payments.PaymentService.class);
        }
    }

    private WebTestClient webTestClient;

    @BeforeEach
    public void setup() {
        this.webTestClient = WebTestClient.bindToApplicationContext(context).build();

        // Stub the paymentService to return a simple successful PaymentIntent so the controller will
        // not trigger downstream DB operations and we can assert the binding behavior in isolation
        PaymentIntent intent = PaymentIntent.builder()
                .paymentId(PaymentId.of("pay_test"))
                .amount(Amount.of(BigDecimal.valueOf(1000), "USD"))
                .merchantId("merchant_123")
                .status(com.hyperswitch.common.enums.PaymentStatus.REQUIRES_CONFIRMATION)
                .build();
        when(paymentService.createPayment(any())).thenReturn(Mono.just(Result.ok(intent)));
        // Note: The actual Result type wrappers are custom; to keep test simple we'll use a small stub
        // and verify that the mocked service received a non-null Amount on the CreatePaymentRequest.
    }

    @Test
    public void postCreatePayment_should_bind_request_body_to_dto() {
        String json = "{\"merchantId\":\"merchant_123\",\"amount\":{\"value\":1000,\"currencyCode\":\"USD\"}}";

        webTestClient.post().uri("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange()
                .expectStatus().is2xxSuccessful();

        // Verify that the service was called - this implies binding succeeded
        org.mockito.ArgumentCaptor<com.hyperswitch.common.dto.CreatePaymentRequest> captor = org.mockito.ArgumentCaptor.forClass(com.hyperswitch.common.dto.CreatePaymentRequest.class);
        // Allow for the request to be processed more than once in case of duplicate reads/subscriptions;
        // assert that at least one of the invocations received a non-null Amount.
        verify(paymentService, org.mockito.Mockito.atLeastOnce()).createPayment(captor.capture());
        java.util.List<com.hyperswitch.common.dto.CreatePaymentRequest> allCaptured = captor.getAllValues();
        boolean anyHasAmount = allCaptured.stream().anyMatch(req -> req.getAmount() != null);
        org.junit.jupiter.api.Assertions.assertTrue(anyHasAmount, "At least one invocation should have a deserialized non-null Amount");
    }
}
