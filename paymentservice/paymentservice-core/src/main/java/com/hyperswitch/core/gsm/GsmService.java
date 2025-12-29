package com.hyperswitch.core.gsm;

import com.hyperswitch.common.dto.*;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
import reactor.core.publisher.Mono;

/**
 * Service interface for GSM (Global Settings Management) operations
 */
public interface GsmService {
    
    /**
     * Create GSM rule
     */
    Mono<Result<GsmResponse, PaymentError>> createGsmRule(GsmCreateRequest request);
    
    /**
     * Get GSM rule
     */
    Mono<Result<GsmResponse, PaymentError>> getGsmRule(GsmRetrieveRequest request);
    
    /**
     * Update GSM rule
     */
    Mono<Result<GsmResponse, PaymentError>> updateGsmRule(GsmUpdateRequest request);
    
    /**
     * Delete GSM rule
     */
    Mono<Result<GsmDeleteResponse, PaymentError>> deleteGsmRule(GsmDeleteRequest request);
}

