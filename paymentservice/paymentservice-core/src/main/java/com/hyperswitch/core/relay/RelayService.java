package com.hyperswitch.core.relay;

import com.hyperswitch.common.dto.RelayRequest;
import com.hyperswitch.common.dto.RelayResponse;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
import reactor.core.publisher.Mono;

/**
 * Service interface for relay operations
 */
public interface RelayService {
    
    /**
     * Create relay request
     */
    Mono<Result<RelayResponse, PaymentError>> createRelay(
            String merchantId,
            String profileId,
            RelayRequest request);
    
    /**
     * Get relay by ID
     */
    Mono<Result<RelayResponse, PaymentError>> getRelay(
            String merchantId,
            String relayId);
}

