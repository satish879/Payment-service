package com.hyperswitch.core.profiles.impl;

import com.hyperswitch.common.dto.ProfileRequest;
import com.hyperswitch.common.dto.ProfileResponse;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
import com.hyperswitch.core.profiles.ProfileService;
import com.hyperswitch.storage.entity.BusinessProfileEntity;
import com.hyperswitch.storage.repository.BusinessProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of ProfileService
 */
@Service
public class ProfileServiceImpl implements ProfileService {
    
    private static final Logger log = LoggerFactory.getLogger(ProfileServiceImpl.class);
    
    private final BusinessProfileRepository profileRepository;
    
    @Autowired
    public ProfileServiceImpl(BusinessProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }
    
    @Override
    public Mono<Result<ProfileResponse, PaymentError>> createProfile(
            String merchantId, 
            ProfileRequest request) {
        
        log.info("Creating profile for merchant: {}, name: {}", merchantId, request.getProfileName());
        
        String profileId = generateProfileId(merchantId);
        
        BusinessProfileEntity entity = new BusinessProfileEntity();
        entity.setProfileId(profileId);
        entity.setMerchantId(merchantId);
        entity.setProfileName(request.getProfileName());
        entity.setReturnUrl(request.getReturnUrl());
        entity.setEnablePaymentResponseHash(
            request.getEnablePaymentResponseHash() != null 
                ? request.getEnablePaymentResponseHash() 
                : Boolean.TRUE);
        entity.setPaymentResponseHashKey(request.getPaymentResponseHashKey());
        entity.setRedirectToMerchantWithHttpPost(
            request.getRedirectToMerchantWithHttpPost() != null 
                ? request.getRedirectToMerchantWithHttpPost() 
                : Boolean.FALSE);
        entity.setWebhookDetails(request.getWebhookDetails());
        entity.setMetadata(request.getMetadata());
        entity.setIsReconEnabled(
            request.getIsReconEnabled() != null 
                ? request.getIsReconEnabled() 
                : Boolean.FALSE);
        entity.setIsExtendedCardInfoEnabled(request.getIsExtendedCardInfoEnabled());
        entity.setIsConnectorAgnosticMitEnabled(request.getIsConnectorAgnosticMitEnabled());
        entity.setVersion("v1");
        entity.setCreatedAt(Instant.now());
        entity.setModifiedAt(Instant.now());
        
        return profileRepository.save(entity)
            .map(this::toProfileResponse)
            .map(Result::<ProfileResponse, PaymentError>ok)
            .onErrorResume(error -> {
                log.error("Error creating profile: {}", error.getMessage(), error);
                return Mono.just(Result.err(PaymentError.of("PROFILE_CREATE_FAILED",
                    "Failed to create profile: " + error.getMessage())));
            });
    }
    
    @Override
    public Mono<Result<ProfileResponse, PaymentError>> getProfile(
            String merchantId, 
            String profileId) {
        
        log.info("Getting profile: {} for merchant: {}", profileId, merchantId);
        
        return profileRepository.findByProfileIdAndMerchantId(profileId, merchantId)
            .map(this::toProfileResponse)
            .map(Result::<ProfileResponse, PaymentError>ok)
            .switchIfEmpty(Mono.just(Result.err(PaymentError.of("PROFILE_NOT_FOUND",
                "Profile not found: " + profileId))))
            .onErrorResume(error -> {
                log.error("Error getting profile: {}", error.getMessage(), error);
                return Mono.just(Result.err(PaymentError.of("PROFILE_RETRIEVAL_FAILED",
                    "Failed to get profile: " + error.getMessage())));
            });
    }
    
    @Override
    public Mono<Result<ProfileResponse, PaymentError>> updateProfile(
            String merchantId, 
            String profileId,
            ProfileRequest request) {
        
        log.info("Updating profile: {} for merchant: {}", profileId, merchantId);
        
        return profileRepository.findByProfileIdAndMerchantId(profileId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException("PROFILE_NOT_FOUND")))
            .flatMap(entity -> {
                if (request.getProfileName() != null) {
                    entity.setProfileName(request.getProfileName());
                }
                if (request.getReturnUrl() != null) {
                    entity.setReturnUrl(request.getReturnUrl());
                }
                if (request.getEnablePaymentResponseHash() != null) {
                    entity.setEnablePaymentResponseHash(request.getEnablePaymentResponseHash());
                }
                if (request.getPaymentResponseHashKey() != null) {
                    entity.setPaymentResponseHashKey(request.getPaymentResponseHashKey());
                }
                if (request.getRedirectToMerchantWithHttpPost() != null) {
                    entity.setRedirectToMerchantWithHttpPost(request.getRedirectToMerchantWithHttpPost());
                }
                if (request.getWebhookDetails() != null) {
                    entity.setWebhookDetails(request.getWebhookDetails());
                }
                if (request.getMetadata() != null) {
                    entity.setMetadata(request.getMetadata());
                }
                if (request.getIsReconEnabled() != null) {
                    entity.setIsReconEnabled(request.getIsReconEnabled());
                }
                if (request.getIsExtendedCardInfoEnabled() != null) {
                    entity.setIsExtendedCardInfoEnabled(request.getIsExtendedCardInfoEnabled());
                }
                if (request.getIsConnectorAgnosticMitEnabled() != null) {
                    entity.setIsConnectorAgnosticMitEnabled(request.getIsConnectorAgnosticMitEnabled());
                }
                entity.setModifiedAt(Instant.now());
                
                return profileRepository.save(entity)
                    .map(this::toProfileResponse)
                    .map(Result::<ProfileResponse, PaymentError>ok);
            })
            .onErrorResume(error -> {
                log.error("Error updating profile: {}", error.getMessage(), error);
                if (error.getMessage() != null && error.getMessage().contains("PROFILE_NOT_FOUND")) {
                    return Mono.just(Result.<ProfileResponse, PaymentError>err(
                        PaymentError.of("PROFILE_NOT_FOUND", "Profile not found: " + profileId)));
                }
                return Mono.just(Result.err(PaymentError.of("PROFILE_UPDATE_FAILED",
                    "Failed to update profile: " + error.getMessage())));
            });
    }
    
    @Override
    public Mono<Result<Flux<ProfileResponse>, PaymentError>> listProfiles(String merchantId) {
        log.info("Listing profiles for merchant: {}", merchantId);
        
        return Mono.just(Result.<Flux<ProfileResponse>, PaymentError>ok(profileRepository.findByMerchantId(merchantId)
            .map(this::toProfileResponse)))
            .onErrorResume(error -> {
                log.error("Error listing profiles: {}", error.getMessage(), error);
                return Mono.just(Result.err(PaymentError.of("PROFILE_LIST_FAILED",
                    "Failed to list profiles: " + error.getMessage())));
            });
    }
    
    @Override
    public Mono<Result<Flux<ProfileResponse>, PaymentError>> listProfilesAtProfileLevel(
            String merchantId, 
            String profileId) {
        
        log.info("Listing profiles at profile level for merchant: {}, profile: {}", merchantId, profileId);
        
        // Filter profiles by merchantId and profileId
        return Mono.just(Result.<Flux<ProfileResponse>, PaymentError>ok(
            profileRepository.findByMerchantId(merchantId)
                .filter(entity -> profileId.equals(entity.getProfileId()))
                .map(this::toProfileResponse)))
            .onErrorResume(error -> {
                log.error("Error listing profiles at profile level: {}", error.getMessage(), error);
                return Mono.just(Result.err(PaymentError.of("PROFILE_LIST_FAILED",
                    "Failed to list profiles at profile level: " + error.getMessage())));
            });
    }
    
    @Override
    public Mono<Result<Void, PaymentError>> deleteProfile(
            String merchantId, 
            String profileId) {
        
        log.info("Deleting profile: {} for merchant: {}", profileId, merchantId);
        
        return profileRepository.findByProfileIdAndMerchantId(profileId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException("PROFILE_NOT_FOUND")))
            .flatMap(entity -> profileRepository.delete(entity)
                .thenReturn(Result.<Void, PaymentError>ok(null)))
            .onErrorResume(error -> {
                log.error("Error deleting profile: {}", error.getMessage(), error);
                if (error.getMessage() != null && error.getMessage().contains("PROFILE_NOT_FOUND")) {
                    return Mono.just(Result.<Void, PaymentError>err(
                        PaymentError.of("PROFILE_NOT_FOUND", "Profile not found: " + profileId)));
                }
                return Mono.just(Result.err(PaymentError.of("PROFILE_DELETE_FAILED",
                    "Failed to delete profile: " + error.getMessage())));
            });
    }
    
    @Override
    public Mono<Result<ProfileResponse, PaymentError>> toggleExtendedCardInfo(
            String merchantId, 
            String profileId) {
        
        log.info("Toggling extended card info for profile: {} merchant: {}", profileId, merchantId);
        
        return profileRepository.findByProfileIdAndMerchantId(profileId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException("PROFILE_NOT_FOUND")))
            .flatMap(entity -> {
                entity.setIsExtendedCardInfoEnabled(
                    entity.getIsExtendedCardInfoEnabled() == null || !entity.getIsExtendedCardInfoEnabled());
                entity.setModifiedAt(Instant.now());
                return profileRepository.save(entity)
                    .map(this::toProfileResponse)
                    .map(Result::<ProfileResponse, PaymentError>ok);
            })
            .onErrorResume(error -> {
                log.error("Error toggling extended card info: {}", error.getMessage(), error);
                if (error.getMessage() != null && error.getMessage().contains("PROFILE_NOT_FOUND")) {
                    return Mono.just(Result.<ProfileResponse, PaymentError>err(
                        PaymentError.of("PROFILE_NOT_FOUND", "Profile not found: " + profileId)));
                }
                return Mono.just(Result.err(PaymentError.of("PROFILE_UPDATE_FAILED",
                    "Failed to toggle extended card info: " + error.getMessage())));
            });
    }
    
    @Override
    public Mono<Result<ProfileResponse, PaymentError>> toggleConnectorAgnosticMit(
            String merchantId, 
            String profileId) {
        
        log.info("Toggling connector agnostic MIT for profile: {} merchant: {}", profileId, merchantId);
        
        return profileRepository.findByProfileIdAndMerchantId(profileId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException("PROFILE_NOT_FOUND")))
            .flatMap(entity -> {
                entity.setIsConnectorAgnosticMitEnabled(
                    entity.getIsConnectorAgnosticMitEnabled() == null || !entity.getIsConnectorAgnosticMitEnabled());
                entity.setModifiedAt(Instant.now());
                return profileRepository.save(entity)
                    .map(this::toProfileResponse)
                    .map(Result::<ProfileResponse, PaymentError>ok);
            })
            .onErrorResume(error -> {
                log.error("Error toggling connector agnostic MIT: {}", error.getMessage(), error);
                if (error.getMessage() != null && error.getMessage().contains("PROFILE_NOT_FOUND")) {
                    return Mono.just(Result.<ProfileResponse, PaymentError>err(
                        PaymentError.of("PROFILE_NOT_FOUND", "Profile not found: " + profileId)));
                }
                return Mono.just(Result.err(PaymentError.of("PROFILE_UPDATE_FAILED",
                    "Failed to toggle connector agnostic MIT: " + error.getMessage())));
            });
    }
    
    @Override
    public Mono<Result<Map<String, Object>, PaymentError>> getFallbackRouting(
            String merchantId, 
            String profileId) {
        
        log.info("Getting fallback routing for profile: {} merchant: {}", profileId, merchantId);
        
        return profileRepository.findByProfileIdAndMerchantId(profileId, merchantId)
            .map(entity -> {
                Map<String, Object> fallbackRouting = entity.getDefaultFallbackRouting();
                return Result.<Map<String, Object>, PaymentError>ok(
                    fallbackRouting != null ? fallbackRouting : Map.of());
            })
            .switchIfEmpty(Mono.just(Result.err(PaymentError.of("PROFILE_NOT_FOUND",
                "Profile not found: " + profileId))))
            .onErrorResume(error -> {
                log.error("Error getting fallback routing: {}", error.getMessage(), error);
                return Mono.just(Result.err(PaymentError.of("ROUTING_RETRIEVAL_FAILED",
                    "Failed to get fallback routing: " + error.getMessage())));
            });
    }
    
    @Override
    public Mono<Result<Map<String, Object>, PaymentError>> updateFallbackRouting(
            String merchantId, 
            String profileId,
            Map<String, Object> fallbackRouting) {
        
        log.info("Updating fallback routing for profile: {} merchant: {}", profileId, merchantId);
        
        return profileRepository.findByProfileIdAndMerchantId(profileId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException("PROFILE_NOT_FOUND")))
            .flatMap(entity -> {
                entity.setDefaultFallbackRouting(fallbackRouting);
                entity.setModifiedAt(Instant.now());
                return profileRepository.save(entity)
                    .map(e -> Result.<Map<String, Object>, PaymentError>ok(
                        e.getDefaultFallbackRouting() != null ? e.getDefaultFallbackRouting() : Map.of()));
            })
            .onErrorResume(error -> {
                log.error("Error updating fallback routing: {}", error.getMessage(), error);
                if (error.getMessage() != null && error.getMessage().contains("PROFILE_NOT_FOUND")) {
                    return Mono.just(Result.<Map<String, Object>, PaymentError>err(
                        PaymentError.of("PROFILE_NOT_FOUND", "Profile not found: " + profileId)));
                }
                return Mono.just(Result.err(PaymentError.of("ROUTING_UPDATE_FAILED",
                    "Failed to update fallback routing: " + error.getMessage())));
            });
    }
    
    @Override
    public Mono<Result<ProfileResponse, PaymentError>> activateRoutingAlgorithm(
            String merchantId, 
            String profileId,
            String algorithmId) {
        
        log.info("Activating routing algorithm: {} for profile: {} merchant: {}", 
                algorithmId, profileId, merchantId);
        
        return profileRepository.findByProfileIdAndMerchantId(profileId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException("PROFILE_NOT_FOUND")))
            .flatMap(entity -> {
                entity.setRoutingAlgorithmId(algorithmId);
                entity.setModifiedAt(Instant.now());
                return profileRepository.save(entity)
                    .map(this::toProfileResponse)
                    .map(Result::<ProfileResponse, PaymentError>ok);
            })
            .onErrorResume(error -> {
                log.error("Error activating routing algorithm: {}", error.getMessage(), error);
                if (error.getMessage() != null && error.getMessage().contains("PROFILE_NOT_FOUND")) {
                    return Mono.just(Result.<ProfileResponse, PaymentError>err(
                        PaymentError.of("PROFILE_NOT_FOUND", "Profile not found: " + profileId)));
                }
                return Mono.just(Result.err(PaymentError.of("ROUTING_ACTIVATION_FAILED",
                    "Failed to activate routing algorithm: " + error.getMessage())));
            });
    }
    
    @Override
    public Mono<Result<ProfileResponse, PaymentError>> deactivateRoutingAlgorithm(
            String merchantId, 
            String profileId) {
        
        log.info("Deactivating routing algorithm for profile: {} merchant: {}", profileId, merchantId);
        
        return profileRepository.findByProfileIdAndMerchantId(profileId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException("PROFILE_NOT_FOUND")))
            .flatMap(entity -> {
                entity.setRoutingAlgorithmId(null);
                entity.setModifiedAt(Instant.now());
                return profileRepository.save(entity)
                    .map(this::toProfileResponse)
                    .map(Result::<ProfileResponse, PaymentError>ok);
            })
            .onErrorResume(error -> {
                log.error("Error deactivating routing algorithm: {}", error.getMessage(), error);
                if (error.getMessage() != null && error.getMessage().contains("PROFILE_NOT_FOUND")) {
                    return Mono.just(Result.<ProfileResponse, PaymentError>err(
                        PaymentError.of("PROFILE_NOT_FOUND", "Profile not found: " + profileId)));
                }
                return Mono.just(Result.err(PaymentError.of("ROUTING_DEACTIVATION_FAILED",
                    "Failed to deactivate routing algorithm: " + error.getMessage())));
            });
    }
    
    @Override
    public Mono<Result<Map<String, Object>, PaymentError>> getRoutingAlgorithm(
            String merchantId, 
            String profileId) {
        
        log.info("Getting routing algorithm for profile: {} merchant: {}", profileId, merchantId);
        
        return profileRepository.findByProfileIdAndMerchantId(profileId, merchantId)
            .map(entity -> {
                Map<String, Object> routingAlgorithm = entity.getRoutingAlgorithm();
                return Result.<Map<String, Object>, PaymentError>ok(
                    routingAlgorithm != null ? routingAlgorithm : Map.of());
            })
            .switchIfEmpty(Mono.just(Result.err(PaymentError.of("PROFILE_NOT_FOUND",
                "Profile not found: " + profileId))))
            .onErrorResume(error -> {
                log.error("Error getting routing algorithm: {}", error.getMessage(), error);
                return Mono.just(Result.err(PaymentError.of("ROUTING_RETRIEVAL_FAILED",
                    "Failed to get routing algorithm: " + error.getMessage())));
            });
    }
    
    @Override
    public Mono<Result<Map<String, Object>, PaymentError>> upsertDecisionManagerConfig(
            String merchantId, 
            String profileId,
            Map<String, Object> decisionConfig) {
        
        log.info("Upserting decision manager config for profile: {} merchant: {}", profileId, merchantId);
        
        // Store decision manager config in profile metadata
        // In production, this could be integrated with a dedicated decision manager service
        return profileRepository.findByProfileIdAndMerchantId(profileId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException("PROFILE_NOT_FOUND")))
            .flatMap(entity -> {
                Map<String, Object> metadata = entity.getMetadata();
                if (metadata == null) {
                    metadata = new java.util.HashMap<>();
                }
                metadata.put("decision_manager_config", decisionConfig);
                entity.setMetadata(metadata);
                entity.setModifiedAt(Instant.now());
                return profileRepository.save(entity)
                    .map(e -> Result.<Map<String, Object>, PaymentError>ok(decisionConfig));
            })
            .onErrorResume(error -> {
                log.error("Error upserting decision manager config: {}", error.getMessage(), error);
                if (error.getMessage() != null && error.getMessage().contains("PROFILE_NOT_FOUND")) {
                    return Mono.just(Result.<Map<String, Object>, PaymentError>err(
                        PaymentError.of("PROFILE_NOT_FOUND", "Profile not found: " + profileId)));
                }
                return Mono.just(Result.err(PaymentError.of("DECISION_CONFIG_UPDATE_FAILED",
                    "Failed to upsert decision manager config: " + error.getMessage())));
            });
    }
    
    @Override
    public Mono<Result<Map<String, Object>, PaymentError>> getDecisionManagerConfig(
            String merchantId, 
            String profileId) {
        
        log.info("Getting decision manager config for profile: {} merchant: {}", profileId, merchantId);
        
        return profileRepository.findByProfileIdAndMerchantId(profileId, merchantId)
            .map(entity -> {
                Map<String, Object> metadata = entity.getMetadata();
                if (metadata != null && metadata.containsKey("decision_manager_config")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> config = (Map<String, Object>) metadata.get("decision_manager_config");
                    return Result.<Map<String, Object>, PaymentError>ok(config);
                }
                return Result.<Map<String, Object>, PaymentError>ok(Map.of());
            })
            .switchIfEmpty(Mono.just(Result.err(PaymentError.of("PROFILE_NOT_FOUND",
                "Profile not found: " + profileId))))
            .onErrorResume(error -> {
                log.error("Error getting decision manager config: {}", error.getMessage(), error);
                return Mono.just(Result.err(PaymentError.of("DECISION_CONFIG_RETRIEVAL_FAILED",
                    "Failed to get decision manager config: " + error.getMessage())));
            });
    }
    
    private ProfileResponse toProfileResponse(BusinessProfileEntity entity) {
        ProfileResponse response = new ProfileResponse();
        response.setProfileId(entity.getProfileId());
        response.setMerchantId(entity.getMerchantId());
        response.setProfileName(entity.getProfileName());
        response.setCreatedAt(entity.getCreatedAt());
        response.setModifiedAt(entity.getModifiedAt());
        response.setReturnUrl(entity.getReturnUrl());
        response.setEnablePaymentResponseHash(entity.getEnablePaymentResponseHash());
        response.setIsReconEnabled(entity.getIsReconEnabled());
        response.setIsExtendedCardInfoEnabled(entity.getIsExtendedCardInfoEnabled());
        response.setIsConnectorAgnosticMitEnabled(entity.getIsConnectorAgnosticMitEnabled());
        response.setMetadata(entity.getMetadata());
        response.setApplepayVerifiedDomains(entity.getApplepayVerifiedDomains());
        return response;
    }
    
    private String generateProfileId(String merchantId) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return merchantId + "_prof_" + uuid.substring(0, 32);
    }
}

