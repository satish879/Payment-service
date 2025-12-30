package com.hyperswitch.core.connectors;

import com.hyperswitch.common.dto.*;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
import com.hyperswitch.core.connectors.impl.ConnectorApiServiceImpl;
import com.hyperswitch.core.connectors.ConnectorHttpClient;
import com.hyperswitch.core.connectors.ConnectorRetryService;
import com.hyperswitch.core.connectors.ConnectorRateLimiter;
import com.hyperswitch.core.connectors.ConnectorCacheService;
import com.hyperswitch.core.test.TestDataBuilders;
import com.hyperswitch.core.test.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConnectorApiService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorApiService Unit Tests")
class ConnectorApiServiceTest {
    
    @Mock
    private ConnectorHttpClient httpClient;
    
    @Mock
    private MerchantConnectorAccountService connectorAccountService;
    
    @Mock
    private ConnectorRetryService retryService;
    
    @Mock
    private ConnectorRateLimiter rateLimiter;
    
    @Mock
    private ConnectorCacheService cacheService;
    
    @InjectMocks
    private ConnectorApiServiceImpl connectorApiService;
    
    private String testPaymentId;
    private String testConnectorName;
    
    @BeforeEach
    void setUp() {
        testPaymentId = TestUtils.generateTestPaymentId();
        testConnectorName = "stripe";
    }
    
    @Test
    @DisplayName("Should create connector session successfully")
    void testCreateConnectorSession_Success() {
        // Given
        ConnectorSessionRequest request = TestDataBuilders.connectorSessionRequest();
        request.setPaymentId(testPaymentId);
        
        Map<String, String> credentials = new HashMap<>();
        credentials.put("api_key", "test_api_key");
        
        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("id", "session_123");
        apiResponse.put("client_secret", "secret_123");
        
        when(rateLimiter.waitIfNeeded(anyString())).thenReturn(Mono.empty());
        when(retryService.executeWithRetry(any(), anyString(), anyString()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                java.util.function.Function<Void, Mono<Result<Map<String, Object>, PaymentError>>> apiCall = 
                    invocation.getArgument(0);
                return apiCall.apply(null);
            });
        when(httpClient.post(anyString(), any(), any(), anyString()))
            .thenReturn(Mono.just(Result.ok(apiResponse)));
        when(httpClient.getConnectorBaseUrl(anyString())).thenReturn("https://api.stripe.com/v1");
        when(httpClient.buildAuthHeaders(anyString(), any())).thenReturn(credentials);
        
        // When
        Mono<Result<ConnectorSessionResponse, PaymentError>> result = 
            connectorApiService.createConnectorSession(testPaymentId, request);
        
        // Then
        StepVerifier.create(result)
            .assertNext(resultValue -> {
                assertThat(resultValue.isOk()).isTrue();
                ConnectorSessionResponse response = resultValue.unwrap();
                assertThat(response).isNotNull();
                assertThat(response.getSessionId()).isNotNull();
                assertThat(response.getConnectorName()).isEqualTo(testConnectorName);
            })
            .verifyComplete();
    }
    
    @Test
    @DisplayName("Should execute payment through connector successfully")
    void testExecutePayment_Success() {
        // Given
        ConnectorPaymentRequest request = TestDataBuilders.connectorPaymentRequest();
        
        Map<String, String> credentials = new HashMap<>();
        credentials.put("api_key", "test_api_key");
        
        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("id", "txn_123");
        apiResponse.put("status", "succeeded");
        
        when(rateLimiter.waitIfNeeded(anyString())).thenReturn(Mono.empty());
        when(retryService.executeWithRetry(any(), anyString(), anyString()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                java.util.function.Function<Void, Mono<Result<Map<String, Object>, PaymentError>>> apiCall = 
                    invocation.getArgument(0);
                return apiCall.apply(null);
            });
        when(httpClient.post(anyString(), any(), any(), anyString()))
            .thenReturn(Mono.just(Result.ok(apiResponse)));
        when(httpClient.getConnectorBaseUrl(anyString())).thenReturn("https://api.stripe.com/v1");
        when(httpClient.buildAuthHeaders(anyString(), any())).thenReturn(credentials);
        when(cacheService.generateKey(anyString(), anyString(), anyString())).thenReturn("cache_key");
        
        // When
        Mono<Result<ConnectorPaymentResponse, PaymentError>> result = 
            connectorApiService.executePayment(testPaymentId, request);
        
        // Then
        StepVerifier.create(result)
            .assertNext(resultValue -> {
                assertThat(resultValue.isOk()).isTrue();
                ConnectorPaymentResponse response = resultValue.unwrap();
                assertThat(response).isNotNull();
                assertThat(response.getPaymentId()).isEqualTo(testPaymentId);
                assertThat(response.getStatus()).isEqualTo("succeeded");
            })
            .verifyComplete();
    }
    
    @Test
    @DisplayName("Should handle connector API error")
    void testExecutePayment_Error() {
        // Given
        ConnectorPaymentRequest request = TestDataBuilders.connectorPaymentRequest();
        
        Map<String, String> credentials = new HashMap<>();
        credentials.put("api_key", "test_api_key");
        
        when(rateLimiter.waitIfNeeded(anyString())).thenReturn(Mono.empty());
        when(retryService.executeWithRetry(any(), anyString(), anyString()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                java.util.function.Function<Void, Mono<Result<Map<String, Object>, PaymentError>>> apiCall = 
                    invocation.getArgument(0);
                return apiCall.apply(null);
            });
        when(httpClient.post(anyString(), any(), any(), anyString()))
            .thenReturn(Mono.just(Result.err(PaymentError.of("CONNECTOR_ERROR", "Connector API error"))));
        when(httpClient.getConnectorBaseUrl(anyString())).thenReturn("https://api.stripe.com/v1");
        when(httpClient.buildAuthHeaders(anyString(), any())).thenReturn(credentials);
        
        // When
        Mono<Result<ConnectorPaymentResponse, PaymentError>> result = 
            connectorApiService.executePayment(testPaymentId, request);
        
        // Then
        StepVerifier.create(result)
            .assertNext(resultValue -> {
                assertThat(resultValue.isErr()).isTrue();
                PaymentError error = resultValue.unwrapErr();
                assertThat(error.getCode()).isEqualTo("CONNECTOR_ERROR");
            })
            .verifyComplete();
    }
}

