package com.hyperswitch.core.payments;

import com.hyperswitch.common.dto.CreatePaymentRequest;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Amount;
import com.hyperswitch.common.types.PaymentId;
import com.hyperswitch.common.types.Result;
import com.hyperswitch.core.payments.PaymentIntent;
import com.hyperswitch.core.payments.impl.PaymentServiceImpl;
import com.hyperswitch.core.test.TestUtils;
import com.hyperswitch.storage.entity.PaymentIntentEntity;
import com.hyperswitch.storage.repository.PaymentIntentRepository;
import com.hyperswitch.storage.repository.PaymentAttemptRepository;
import com.hyperswitch.storage.repository.RefundRepository;
import com.hyperswitch.storage.repository.RoutingDecisionLogRepository;
import com.hyperswitch.routing.RoutingService;
import com.hyperswitch.connectors.ConnectorService;
import com.hyperswitch.core.payments.impl.PaymentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PaymentService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {
    
    @Mock
    private PaymentIntentRepository paymentIntentRepository;
    
    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;
    
    @Mock
    private RefundRepository refundRepository;
    
    @Mock
    private RoutingService routingService;
    
    @Mock
    private ConnectorService connectorService;
    
    @Mock
    private RoutingDecisionLogRepository routingDecisionLogRepository;
    
    @Mock
    private PaymentMapper paymentMapper;
    
    @Mock
    private com.hyperswitch.core.mandates.MandateService mandateService;
    
    @Mock
    private com.hyperswitch.core.metrics.PaymentMetrics paymentMetrics;
    
    @Mock
    private com.hyperswitch.core.analytics.AnalyticsService analyticsService;
    
    @Mock
    private org.springframework.transaction.reactive.TransactionalOperator transactionalOperator;
    
    @InjectMocks
    private PaymentServiceImpl paymentService;
    
    private String testMerchantId;
    private String testPaymentId;
    
    @BeforeEach
    void setUp() {
        testMerchantId = TestUtils.generateTestMerchantId();
        testPaymentId = TestUtils.generateTestPaymentId();
        
        // Setup TransactionalOperator to pass through the Mono
        when(transactionalOperator.transactional(any(reactor.core.publisher.Mono.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }
    
    @Test
    @DisplayName("Should create payment successfully")
    void testCreatePayment_Success() {
        // Given
        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .merchantId(testMerchantId)
            .amount(Amount.of(java.math.BigDecimal.valueOf(1000L), "USD"))
            .customerId("test_customer")
            .description("Test payment")
            .build();
        
        PaymentIntentEntity mockEntity = createMockPaymentEntity();
        PaymentIntent mockPaymentIntent = PaymentIntent.builder()
            .paymentId(PaymentId.of(testPaymentId))
            .merchantId(testMerchantId)
            .amount(Amount.of(java.math.BigDecimal.valueOf(1000L), "USD"))
            .status(com.hyperswitch.common.enums.PaymentStatus.REQUIRES_CONFIRMATION)
            .build();
        
        when(paymentIntentRepository.save(any(PaymentIntentEntity.class)))
            .thenReturn(Mono.just(mockEntity));
        when(paymentMapper.toPaymentIntent(any(PaymentIntentEntity.class)))
            .thenReturn(mockPaymentIntent);
        
        // When
        Mono<Result<PaymentIntent, PaymentError>> result = paymentService.createPayment(request);
        
        // Then
        StepVerifier.create(result)
            .assertNext(resultValue -> {
                assertThat(resultValue.isOk()).isTrue();
                PaymentIntent paymentIntent = resultValue.unwrap();
                assertThat(paymentIntent).isNotNull();
                assertThat(paymentIntent.getPaymentId()).isNotNull();
            })
            .verifyComplete();
    }
    
    @Test
    @DisplayName("Should handle payment creation error")
    void testCreatePayment_Error() {
        // Given
        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .merchantId(testMerchantId)
            .amount(com.hyperswitch.common.types.Amount.of(java.math.BigDecimal.valueOf(1000L), "USD"))
            .build();
        
        when(paymentIntentRepository.save(any(PaymentIntentEntity.class)))
            .thenReturn(Mono.error(new RuntimeException("Database error")));
        
        // When
        Mono<Result<PaymentIntent, PaymentError>> result = paymentService.createPayment(request);
        
        // Then
        StepVerifier.create(result)
            .assertNext(resultValue -> {
                assertThat(resultValue.isErr()).isTrue();
                PaymentError error = resultValue.unwrapErr();
                assertThat(error).isNotNull();
            })
            .verifyComplete();
    }
    
    // Helper method to create mock payment entity
    private PaymentIntentEntity createMockPaymentEntity() {
        PaymentIntentEntity entity = new PaymentIntentEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setPaymentId(testPaymentId);
        entity.setMerchantId(testMerchantId);
        entity.setAmount(1000L);
        entity.setCurrency("USD");
        entity.setStatus("requires_confirmation");
        entity.setCreatedAt(Instant.now());
        entity.setModifiedAt(Instant.now());
        return entity;
    }
}

