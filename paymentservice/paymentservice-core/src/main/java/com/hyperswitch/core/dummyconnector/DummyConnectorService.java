package com.hyperswitch.core.dummyconnector;

import com.hyperswitch.common.dto.*;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
import reactor.core.publisher.Mono;

/**
 * Service interface for dummy connector operations (testing)
 */
public interface DummyConnectorService {
    
    /**
     * Create dummy payment
     */
    Mono<Result<DummyConnectorPaymentResponse, PaymentError>> createPayment(
            DummyConnectorPaymentRequest request);
    
    /**
     * Get dummy payment data
     */
    Mono<Result<DummyConnectorPaymentResponse, PaymentError>> getPaymentData(String paymentId);
    
    /**
     * Create dummy refund
     */
    Mono<Result<DummyConnectorRefundResponse, PaymentError>> createRefund(
            String paymentId,
            DummyConnectorRefundRequest request);
    
    /**
     * Get dummy refund data
     */
    Mono<Result<DummyConnectorRefundResponse, PaymentError>> getRefundData(String refundId);
    
    /**
     * Authorize dummy payment
     */
    Mono<Result<String, PaymentError>> authorizePayment(String attemptId);
    
    /**
     * Complete dummy payment
     */
    Mono<Result<Void, PaymentError>> completePayment(String attemptId, Boolean confirm);
}

