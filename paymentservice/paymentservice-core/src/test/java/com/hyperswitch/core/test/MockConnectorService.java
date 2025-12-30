package com.hyperswitch.core.test;

import com.hyperswitch.common.dto.ConnectorAuthorizationRequest;
import com.hyperswitch.common.dto.ConnectorAuthorizationResponse;
import com.hyperswitch.common.dto.ConnectorCaptureRequest;
import com.hyperswitch.common.dto.ConnectorCaptureResponse;
import com.hyperswitch.common.dto.ConnectorPaymentRequest;
import com.hyperswitch.common.dto.ConnectorPaymentResponse;
import com.hyperswitch.common.dto.ConnectorPaymentStatusResponse;
import com.hyperswitch.common.dto.ConnectorRefundRequest;
import com.hyperswitch.common.dto.ConnectorRefundResponse;
import com.hyperswitch.common.dto.ConnectorSessionRequest;
import com.hyperswitch.common.dto.ConnectorSessionResponse;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
import com.hyperswitch.core.connectors.ConnectorApiService;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mock connector service for testing
 * Simulates connector API responses without making actual HTTP calls
 */
public class MockConnectorService implements ConnectorApiService {
    
    private final Map<String, ConnectorSessionResponse> sessionCache = new HashMap<>();
    private final Map<String, ConnectorPaymentResponse> paymentCache = new HashMap<>();
    private final Map<String, ConnectorPaymentStatusResponse> statusCache = new HashMap<>();
    
    @Override
    public Mono<Result<ConnectorSessionResponse, PaymentError>> createConnectorSession(
            String paymentId, ConnectorSessionRequest request) {
        ConnectorSessionResponse response = new ConnectorSessionResponse();
        response.setSessionId("session_" + UUID.randomUUID().toString());
        response.setConnectorName(request.getConnectorName());
        response.setPaymentId(paymentId);
        response.setSessionToken("token_" + UUID.randomUUID().toString());
        response.setExpiresAt(System.currentTimeMillis() + 3600000);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("mock", true);
        response.setMetadata(metadata);
        
        sessionCache.put(paymentId, response);
        return Mono.just(Result.ok(response));
    }
    
    @Override
    public Mono<Result<ConnectorSessionResponse, PaymentError>> createConnectorSession(
            ConnectorSessionRequest request) {
        return createConnectorSession(request.getPaymentId(), request);
    }
    
    @Override
    public Mono<Result<ConnectorPaymentResponse, PaymentError>> executePayment(
            String paymentId, ConnectorPaymentRequest request) {
        ConnectorPaymentResponse response = new ConnectorPaymentResponse();
        response.setPaymentId(paymentId);
        response.setConnectorName(request.getConnectorName());
        response.setStatus("succeeded");
        response.setConnectorTransactionId("txn_" + UUID.randomUUID().toString());
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("mock", true);
        response.setMetadata(metadata);
        
        paymentCache.put(paymentId, response);
        return Mono.just(Result.ok(response));
    }
    
    @Override
    public Mono<Result<ConnectorAuthorizationResponse, PaymentError>> authorizePayment(
            String paymentId, ConnectorAuthorizationRequest request) {
        ConnectorAuthorizationResponse response = new ConnectorAuthorizationResponse();
        response.setPaymentId(paymentId);
        response.setConnectorName(request.getConnectorName());
        response.setStatus("authorized");
        response.setAuthorizationId("auth_" + UUID.randomUUID().toString());
        return Mono.just(Result.ok(response));
    }
    
    @Override
    public Mono<Result<ConnectorCaptureResponse, PaymentError>> capturePayment(
            String paymentId, ConnectorCaptureRequest request) {
        ConnectorCaptureResponse response = new ConnectorCaptureResponse();
        response.setPaymentId(paymentId);
        response.setConnectorName(request.getConnectorName());
        response.setStatus("captured");
        response.setCaptureId("capture_" + UUID.randomUUID().toString());
        return Mono.just(Result.ok(response));
    }
    
    @Override
    public Mono<Result<ConnectorRefundResponse, PaymentError>> processRefund(
            String refundId, ConnectorRefundRequest request) {
        ConnectorRefundResponse response = new ConnectorRefundResponse();
        response.setRefundId(refundId);
        response.setConnectorName(request.getConnectorName());
        response.setStatus("succeeded");
        response.setConnectorRefundId("refund_" + UUID.randomUUID().toString());
        return Mono.just(Result.ok(response));
    }
    
    @Override
    public Mono<Result<ConnectorPaymentStatusResponse, PaymentError>> getPaymentStatus(
            String paymentId, String connectorName) {
        ConnectorPaymentStatusResponse response = statusCache.getOrDefault(paymentId, 
            createDefaultStatusResponse(paymentId, connectorName));
        return Mono.just(Result.ok(response));
    }
    
    @Override
    public Mono<Result<ConnectorPaymentStatusResponse, PaymentError>> syncPaymentStatus(
            String paymentId, String connectorName) {
        ConnectorPaymentStatusResponse response = createDefaultStatusResponse(paymentId, connectorName);
        statusCache.put(paymentId, response);
        return Mono.just(Result.ok(response));
    }
    
    private ConnectorPaymentStatusResponse createDefaultStatusResponse(String paymentId, String connectorName) {
        ConnectorPaymentStatusResponse response = new ConnectorPaymentStatusResponse();
        response.setPaymentId(paymentId);
        response.setConnectorName(connectorName);
        response.setStatus("succeeded");
        response.setConnectorTransactionId("txn_" + UUID.randomUUID().toString());
        return response;
    }
    
    /**
     * Clear all caches
     */
    public void clearCaches() {
        sessionCache.clear();
        paymentCache.clear();
        statusCache.clear();
    }
    
    /**
     * Set a mock payment status
     */
    public void setPaymentStatus(String paymentId, String status) {
        ConnectorPaymentStatusResponse response = new ConnectorPaymentStatusResponse();
        response.setPaymentId(paymentId);
        response.setStatus(status);
        statusCache.put(paymentId, response);
    }
}

