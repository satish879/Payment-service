package com.hyperswitch.core.profiles;

import com.hyperswitch.common.dto.ProfileRequest;
import com.hyperswitch.common.dto.ProfileResponse;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Service interface for profile operations
 */
public interface ProfileService {
    
    /**
     * Create profile
     */
    Mono<Result<ProfileResponse, PaymentError>> createProfile(
            String merchantId, 
            ProfileRequest request);
    
    /**
     * Get profile
     */
    Mono<Result<ProfileResponse, PaymentError>> getProfile(
            String merchantId, 
            String profileId);
    
    /**
     * Update profile
     */
    Mono<Result<ProfileResponse, PaymentError>> updateProfile(
            String merchantId, 
            String profileId,
            ProfileRequest request);
    
    /**
     * List profiles
     */
    Mono<Result<Flux<ProfileResponse>, PaymentError>> listProfiles(String merchantId);
    
    /**
     * Delete profile
     */
    Mono<Result<Void, PaymentError>> deleteProfile(
            String merchantId, 
            String profileId);
    
    /**
     * Toggle extended card info
     */
    Mono<Result<ProfileResponse, PaymentError>> toggleExtendedCardInfo(
            String merchantId, 
            String profileId);
    
    /**
     * Toggle connector agnostic MIT
     */
    Mono<Result<ProfileResponse, PaymentError>> toggleConnectorAgnosticMit(
            String merchantId, 
            String profileId);
    
    /**
     * Get fallback routing
     */
    Mono<Result<Map<String, Object>, PaymentError>> getFallbackRouting(
            String merchantId, 
            String profileId);
    
    /**
     * Update fallback routing
     */
    Mono<Result<Map<String, Object>, PaymentError>> updateFallbackRouting(
            String merchantId, 
            String profileId,
            Map<String, Object> fallbackRouting);
    
    /**
     * Activate routing algorithm
     */
    Mono<Result<ProfileResponse, PaymentError>> activateRoutingAlgorithm(
            String merchantId, 
            String profileId,
            String algorithmId);
    
    /**
     * Deactivate routing algorithm
     */
    Mono<Result<ProfileResponse, PaymentError>> deactivateRoutingAlgorithm(
            String merchantId, 
            String profileId);
    
    /**
     * Get routing algorithm
     */
    Mono<Result<Map<String, Object>, PaymentError>> getRoutingAlgorithm(
            String merchantId, 
            String profileId);
    
    /**
     * Upsert decision manager config
     */
    Mono<Result<Map<String, Object>, PaymentError>> upsertDecisionManagerConfig(
            String merchantId, 
            String profileId,
            Map<String, Object> decisionConfig);
    
    /**
     * Get decision manager config
     */
    Mono<Result<Map<String, Object>, PaymentError>> getDecisionManagerConfig(
            String merchantId, 
            String profileId);
    
    /**
     * List profiles at profile level (filtered by profile ID from auth context)
     */
    Mono<Result<Flux<ProfileResponse>, PaymentError>> listProfilesAtProfileLevel(
            String merchantId, 
            String profileId);
}

