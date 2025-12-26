package com.hyperswitch.core.apikeys;

import com.hyperswitch.common.dto.ApiKeyRequest;
import com.hyperswitch.common.dto.ApiKeyResponse;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.errors.Result;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service interface for API key operations
 */
public interface ApiKeyService {
    
    /**
     * Create API key
     */
    Mono<Result<ApiKeyResponse, PaymentError>> createApiKey(
            String merchantId, 
            ApiKeyRequest request);
    
    /**
     * List API keys
     */
    Mono<Result<Flux<ApiKeyResponse>, PaymentError>> listApiKeys(String merchantId);
    
    /**
     * Get API key
     */
    Mono<Result<ApiKeyResponse, PaymentError>> getApiKey(
            String merchantId, 
            String keyId);
    
    /**
     * Update API key
     */
    Mono<Result<ApiKeyResponse, PaymentError>> updateApiKey(
            String merchantId, 
            String keyId,
            ApiKeyRequest request);
    
    /**
     * Revoke API key
     */
    Mono<Result<Void, PaymentError>> revokeApiKey(
            String merchantId, 
            String keyId);
}

