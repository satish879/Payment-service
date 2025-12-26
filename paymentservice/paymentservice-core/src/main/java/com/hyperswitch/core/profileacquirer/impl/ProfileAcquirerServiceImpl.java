package com.hyperswitch.core.profileacquirer.impl;

import com.hyperswitch.common.dto.ProfileAcquirerRequest;
import com.hyperswitch.common.dto.ProfileAcquirerResponse;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
import com.hyperswitch.core.profileacquirer.ProfileAcquirerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Implementation of ProfileAcquirerService
 */
@Service
public class ProfileAcquirerServiceImpl implements ProfileAcquirerService {
    
    private static final Logger log = LoggerFactory.getLogger(ProfileAcquirerServiceImpl.class);
    
    @Override
    public Mono<Result<ProfileAcquirerResponse, PaymentError>> createProfileAcquirer(
            String merchantId,
            String profileId,
            ProfileAcquirerRequest request) {
        
        log.info("Creating profile acquirer for merchant: {}, profile: {}, acquirer: {}", 
                merchantId, profileId, request.getAcquirerId());
        
        return Mono.fromCallable(() -> {
            String profileAcquirerId = generateProfileAcquirerId(merchantId, profileId);
            
            ProfileAcquirerResponse response = new ProfileAcquirerResponse();
            response.setProfileAcquirerId(profileAcquirerId);
            response.setProfileId(profileId);
            response.setAcquirerId(request.getAcquirerId());
            response.setAcquirerName(request.getAcquirerName());
            response.setConfig(request.getConfig());
            response.setMetadata(request.getMetadata());
            response.setCreatedAt(Instant.now());
            response.setUpdatedAt(Instant.now());
            
            // In production, this would:
            // 1. Validate profile exists
            // 2. Store profile acquirer in database
            // 3. Return created profile acquirer
            
            return Result.<ProfileAcquirerResponse, PaymentError>ok(response);
        })
        .onErrorResume(error -> {
            log.error("Error creating profile acquirer: {}", error.getMessage(), error);
            return Mono.just(Result.err(PaymentError.of("PROFILE_ACQUIRER_CREATE_FAILED",
                "Failed to create profile acquirer: " + error.getMessage())));
        });
    }
    
    @Override
    public Mono<Result<ProfileAcquirerResponse, PaymentError>> updateProfileAcquirer(
            String merchantId,
            String profileId,
            String profileAcquirerId,
            ProfileAcquirerRequest request) {
        
        log.info("Updating profile acquirer: {} for merchant: {}, profile: {}", 
                profileAcquirerId, merchantId, profileId);
        
        return Mono.fromCallable(() -> {
            ProfileAcquirerResponse response = new ProfileAcquirerResponse();
            response.setProfileAcquirerId(profileAcquirerId);
            response.setProfileId(profileId);
            response.setAcquirerId(request.getAcquirerId() != null ? request.getAcquirerId() : "");
            response.setAcquirerName(request.getAcquirerName());
            response.setConfig(request.getConfig());
            response.setMetadata(request.getMetadata());
            response.setUpdatedAt(Instant.now());
            
            // In production, this would:
            // 1. Validate profile acquirer exists
            // 2. Update database record
            // 3. Return updated profile acquirer
            
            return Result.<ProfileAcquirerResponse, PaymentError>ok(response);
        })
        .onErrorResume(error -> {
            log.error("Error updating profile acquirer: {}", error.getMessage(), error);
            return Mono.just(Result.err(PaymentError.of("PROFILE_ACQUIRER_UPDATE_FAILED",
                "Failed to update profile acquirer: " + error.getMessage())));
        });
    }
    
    private String generateProfileAcquirerId(String merchantId, String profileId) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return merchantId + "_" + profileId + "_acq_" + uuid.substring(0, 24);
    }
}

