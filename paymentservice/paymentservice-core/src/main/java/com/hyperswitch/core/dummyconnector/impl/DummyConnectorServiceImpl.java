package com.hyperswitch.core.dummyconnector.impl;

import com.hyperswitch.common.dto.*;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
import com.hyperswitch.core.dummyconnector.DummyConnectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of DummyConnectorService
 * This is a testing connector that simulates payment and refund operations
 */
@Service
public class DummyConnectorServiceImpl implements DummyConnectorService {
    
    private static final Logger log = LoggerFactory.getLogger(DummyConnectorServiceImpl.class);
    
    // In-memory storage for testing (in production, this would use Redis)
    private final Map<String, DummyConnectorPaymentResponse> paymentStore = new HashMap<>();
    private final Map<String, DummyConnectorRefundResponse> refundStore = new HashMap<>();
    private final Map<String, String> attemptToPaymentId = new HashMap<>();
    
    @Override
    public Mono<Result<DummyConnectorPaymentResponse, PaymentError>> createPayment(
            DummyConnectorPaymentRequest request) {
        
        log.info("Creating dummy payment: amount={}, currency={}, connector={}", 
                request.getAmount(), request.getCurrency(), request.getConnector());
        
        return Mono.fromCallable(() -> {
            String paymentId = "pay_" + UUID.randomUUID().toString().replace("-", "");
            String attemptId = "attempt_" + UUID.randomUUID().toString().replace("-", "");
            
            DummyConnectorPaymentResponse response = new DummyConnectorPaymentResponse();
            response.setId(paymentId);
            response.setStatus("processing");
            response.setAmount(request.getAmount());
            response.setCurrency(request.getCurrency());
            response.setCreated(Instant.now());
            response.setPaymentMethodType("card");
            
            // Set next action for 3DS flow
            Map<String, Object> nextAction = new HashMap<>();
            nextAction.put("type", "redirect_to_url");
            nextAction.put("url", "/dummy-connector/authorize/" + attemptId);
            response.setNextAction(nextAction);
            
            // Store payment data
            paymentStore.put(paymentId, response);
            attemptToPaymentId.put(attemptId, paymentId);
            
            // In production, this would:
            // 1. Store payment attempt in Redis with TTL
            // 2. Store payment data in Redis with TTL
            // 3. Process payment based on connector type and payment method
            
            return Result.<DummyConnectorPaymentResponse, PaymentError>ok(response);
        })
        .onErrorResume(error -> {
            log.error("Error creating dummy payment: {}", error.getMessage(), error);
            return Mono.just(Result.err(PaymentError.of("DUMMY_PAYMENT_CREATE_FAILED",
                "Failed to create dummy payment: " + error.getMessage())));
        });
    }
    
    @Override
    public Mono<Result<DummyConnectorPaymentResponse, PaymentError>> getPaymentData(String paymentId) {
        log.info("Getting dummy payment data: {}", paymentId);
        
        return Mono.fromCallable(() -> {
            DummyConnectorPaymentResponse payment = paymentStore.get(paymentId);
            if (payment == null) {
                return Result.<DummyConnectorPaymentResponse, PaymentError>err(
                    PaymentError.of("PAYMENT_NOT_FOUND", "Payment not found: " + paymentId));
            }
            
            // In production, this would retrieve from Redis
            
            return Result.<DummyConnectorPaymentResponse, PaymentError>ok(payment);
        })
        .onErrorResume(error -> {
            log.error("Error getting dummy payment data: {}", error.getMessage(), error);
            return Mono.just(Result.err(PaymentError.of("DUMMY_PAYMENT_RETRIEVAL_FAILED",
                "Failed to get dummy payment data: " + error.getMessage())));
        });
    }
    
    @Override
    public Mono<Result<DummyConnectorRefundResponse, PaymentError>> createRefund(
            String paymentId,
            DummyConnectorRefundRequest request) {
        
        log.info("Creating dummy refund: paymentId={}, amount={}", paymentId, request.getAmount());
        
        return Mono.fromCallable(() -> {
            DummyConnectorPaymentResponse payment = paymentStore.get(paymentId);
            if (payment == null) {
                return Result.<DummyConnectorRefundResponse, PaymentError>err(
                    PaymentError.of("PAYMENT_NOT_FOUND", "Payment not found: " + paymentId));
            }
            
            if (!"succeeded".equals(payment.getStatus())) {
                return Result.<DummyConnectorRefundResponse, PaymentError>err(
                    PaymentError.of("PAYMENT_NOT_SUCCESSFUL", "Payment is not successful"));
            }
            
            if (request.getAmount() > payment.getAmount()) {
                return Result.<DummyConnectorRefundResponse, PaymentError>err(
                    PaymentError.of("REFUND_AMOUNT_EXCEEDS_PAYMENT", 
                        "Refund amount exceeds payment amount"));
            }
            
            String refundId = "refund_" + UUID.randomUUID().toString().replace("-", "");
            
            DummyConnectorRefundResponse response = new DummyConnectorRefundResponse();
            response.setId(refundId);
            response.setStatus("succeeded");
            response.setCurrency(payment.getCurrency());
            response.setCreated(Instant.now());
            response.setPaymentAmount(payment.getAmount());
            response.setRefundAmount(request.getAmount());
            
            refundStore.put(refundId, response);
            
            // In production, this would:
            // 1. Validate payment exists and is refundable
            // 2. Store refund in Redis
            // 3. Update payment eligible amount
            
            return Result.<DummyConnectorRefundResponse, PaymentError>ok(response);
        })
        .onErrorResume(error -> {
            log.error("Error creating dummy refund: {}", error.getMessage(), error);
            return Mono.just(Result.err(PaymentError.of("DUMMY_REFUND_CREATE_FAILED",
                "Failed to create dummy refund: " + error.getMessage())));
        });
    }
    
    @Override
    public Mono<Result<DummyConnectorRefundResponse, PaymentError>> getRefundData(String refundId) {
        log.info("Getting dummy refund data: {}", refundId);
        
        return Mono.fromCallable(() -> {
            DummyConnectorRefundResponse refund = refundStore.get(refundId);
            if (refund == null) {
                return Result.<DummyConnectorRefundResponse, PaymentError>err(
                    PaymentError.of("REFUND_NOT_FOUND", "Refund not found: " + refundId));
            }
            
            // In production, this would retrieve from Redis
            
            return Result.<DummyConnectorRefundResponse, PaymentError>ok(refund);
        })
        .onErrorResume(error -> {
            log.error("Error getting dummy refund data: {}", error.getMessage(), error);
            return Mono.just(Result.err(PaymentError.of("DUMMY_REFUND_RETRIEVAL_FAILED",
                "Failed to get dummy refund data: " + error.getMessage())));
        });
    }
    
    @Override
    public Mono<Result<String, PaymentError>> authorizePayment(String attemptId) {
        log.info("Authorizing dummy payment: attemptId={}", attemptId);
        
        return Mono.fromCallable(() -> {
            String paymentId = attemptToPaymentId.get(attemptId);
            if (paymentId == null) {
                return Result.<String, PaymentError>err(
                    PaymentError.of("ATTEMPT_NOT_FOUND", "Payment attempt not found: " + attemptId));
            }
            
            // In production, this would:
            // 1. Retrieve payment attempt from Redis
            // 2. Generate authorization HTML page
            // 3. Return HTML for 3DS flow
            
            String htmlPage = "<html><body><h1>Dummy Connector Authorization</h1>" +
                    "<p>Payment ID: " + paymentId + "</p>" +
                    "<p>Attempt ID: " + attemptId + "</p>" +
                    "<form action=\"/api/dummy-connector/complete/" + attemptId + "?confirm=true\" method=\"GET\">" +
                    "<button type=\"submit\">Confirm Payment</button></form>" +
                    "<form action=\"/api/dummy-connector/complete/" + attemptId + "?confirm=false\" method=\"GET\">" +
                    "<button type=\"submit\">Cancel Payment</button></form>" +
                    "</body></html>";
            
            return Result.<String, PaymentError>ok(htmlPage);
        })
        .onErrorResume(error -> {
            log.error("Error authorizing dummy payment: {}", error.getMessage(), error);
            return Mono.just(Result.err(PaymentError.of("DUMMY_PAYMENT_AUTHORIZE_FAILED",
                "Failed to authorize dummy payment: " + error.getMessage())));
        });
    }
    
    @Override
    public Mono<Result<Void, PaymentError>> completePayment(String attemptId, Boolean confirm) {
        log.info("Completing dummy payment: attemptId={}, confirm={}", attemptId, confirm);
        
        return Mono.fromCallable(() -> {
            String paymentId = attemptToPaymentId.get(attemptId);
            if (paymentId == null) {
                return Result.<Void, PaymentError>err(
                    PaymentError.of("ATTEMPT_NOT_FOUND", "Payment attempt not found: " + attemptId));
            }
            
            DummyConnectorPaymentResponse payment = paymentStore.get(paymentId);
            if (payment == null) {
                return Result.<Void, PaymentError>err(
                    PaymentError.of("PAYMENT_NOT_FOUND", "Payment not found: " + paymentId));
            }
            
            // Update payment status based on confirmation
            payment.setStatus(confirm ? "succeeded" : "failed");
            payment.setNextAction(null);
            paymentStore.put(paymentId, payment);
            
            // In production, this would:
            // 1. Update payment status in Redis
            // 2. Remove attempt from Redis
            // 3. Redirect to return URL
            
            return Result.<Void, PaymentError>ok(null);
        })
        .onErrorResume(error -> {
            log.error("Error completing dummy payment: {}", error.getMessage(), error);
            return Mono.just(Result.err(PaymentError.of("DUMMY_PAYMENT_COMPLETE_FAILED",
                "Failed to complete dummy payment: " + error.getMessage())));
        });
    }
}

