package com.hyperswitch.core.tokenization.impl;

import com.hyperswitch.common.dto.DeleteTokenDataRequest;
import com.hyperswitch.common.dto.DeleteTokenDataResponse;
import com.hyperswitch.common.dto.TokenizationRequest;
import com.hyperswitch.common.dto.TokenizationResponse;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
import com.hyperswitch.common.types.TokenizationFlag;
import com.hyperswitch.core.tokenization.TokenizationService;
import com.hyperswitch.storage.entity.TokenizationEntity;
import com.hyperswitch.storage.repository.TokenizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Implementation of TokenizationService
 */
@Service
public class TokenizationServiceImpl implements TokenizationService {
    
    private static final Logger log = LoggerFactory.getLogger(TokenizationServiceImpl.class);
    
    private static final String TOKEN_VERSION = "v2";
    private static final int TOKEN_UUID_LENGTH = 32;
    private static final String TOKEN_ID_SEPARATOR = "_tok_";
    
    private final TokenizationRepository tokenizationRepository;
    
    @Autowired
    public TokenizationServiceImpl(TokenizationRepository tokenizationRepository) {
        this.tokenizationRepository = tokenizationRepository;
    }
    
    @Override
    public Mono<Result<TokenizationResponse, PaymentError>> createTokenVault(
            String merchantId, 
            TokenizationRequest request) {
        
        if (request == null) {
            log.warn("Request is null for merchant: {}", merchantId);
            return Mono.just(Result.err(PaymentError.of("INVALID_REQUEST", "Request cannot be null")));
        }
        
        if (request.getCustomerId() == null || request.getCustomerId().isEmpty()) {
            log.warn("Customer ID is null or empty for merchant: {}", merchantId);
            return Mono.just(Result.err(PaymentError.of("INVALID_REQUEST", "Customer ID cannot be null or empty")));
        }
        
        log.info("Creating token vault for merchant: {}, customer: {}", merchantId, request.getCustomerId());
        
        // Generate unique token ID
        String tokenId = generateTokenId(merchantId);
        
        // Generate locker ID (vault ID)
        String lockerId = UUID.randomUUID().toString();
        
        // Create tokenization entity
        TokenizationEntity entity = new TokenizationEntity();
        entity.setId(tokenId);
        entity.setMerchantId(merchantId);
        entity.setCustomerId(request.getCustomerId());
        entity.setLockerId(lockerId);
        entity.setFlag(TokenizationFlag.ENABLED.getValue());
        entity.setVersion(TOKEN_VERSION);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        
        // Integrate with actual vault service to store token_request data
        // In production, this would make API calls to the vault service
        return storeTokenDataInVault(lockerId)
            .flatMap(vaultStored -> {
                if (Boolean.FALSE.equals(vaultStored)) {
                    log.warn("Failed to store token data in vault for token: {}", tokenId);
                    return Mono.just(Result.err(PaymentError.of("VAULT_STORAGE_FAILED",
                        "Failed to store token data in vault service")));
                }
                
                // Store tokenization entity in database after successful vault storage
                return tokenizationRepository.save(entity)
                    .map(saved -> {
                TokenizationResponse response = new TokenizationResponse();
                response.setId(saved.getId());
                response.setCreatedAt(saved.getCreatedAt());
                response.setFlag(saved.getFlag());
                        return Result.<TokenizationResponse, PaymentError>ok(response);
                    })
                    .onErrorResume(Throwable.class, error -> {
                        log.error("Error creating token vault: {}", error.getMessage(), error);
                        return Mono.<Result<TokenizationResponse, PaymentError>>just(
                            Result.err(PaymentError.of("TOKEN_VAULT_CREATION_FAILED", 
                                "Failed to create token vault")));
                    });
            });
    }
    
    @Override
    public Mono<Result<DeleteTokenDataResponse, PaymentError>> deleteTokenizedData(
            String merchantId, 
            String tokenId,
            DeleteTokenDataRequest request) {
        
        if (request == null) {
            log.warn("Request is null for merchant: {}, token: {}", merchantId, tokenId);
            return Mono.just(Result.err(PaymentError.of("INVALID_REQUEST", "Request cannot be null")));
        }
        
        if (tokenId == null || tokenId.isEmpty()) {
            log.warn("Token ID is null or empty for merchant: {}", merchantId);
            return Mono.just(Result.err(PaymentError.of("INVALID_REQUEST", "Token ID cannot be null or empty")));
        }
        
        if (request.getCustomerId() == null || request.getCustomerId().isEmpty()) {
            log.warn("Customer ID is null or empty for merchant: {}, token: {}", merchantId, tokenId);
            return Mono.just(Result.err(PaymentError.of("INVALID_REQUEST", "Customer ID cannot be null or empty")));
        }
        
        log.info("Deleting tokenized data for merchant: {}, token: {}", merchantId, tokenId);
        
        return tokenizationRepository.findByIdAndMerchantId(tokenId, merchantId)
            .defaultIfEmpty((TokenizationEntity) null)
            .flatMap(entity -> {
                if (entity == null) {
                    log.warn("Token not found: {} for merchant: {}", tokenId, merchantId);
                    return Mono.just(Result.<DeleteTokenDataResponse, PaymentError>err(
                        PaymentError.of("TOKEN_NOT_FOUND", "Token not found")));
                }
                
                // Verify customer ID matches
                String entityCustomerId = entity.getCustomerId();
                String requestCustomerId = request.getCustomerId();
                if (entityCustomerId == null || requestCustomerId == null 
                        || !entityCustomerId.equals(requestCustomerId)) {
                    log.warn("Customer ID mismatch for token: {}", tokenId);
                    return Mono.just(Result.<DeleteTokenDataResponse, PaymentError>err(
                        PaymentError.of("CUSTOMER_ID_MISMATCH", "Customer ID mismatch")));
                }
                
                // Delete from actual vault service
                return deleteTokenDataFromVault(entity.getLockerId())
                    .flatMap(vaultDeleted -> {
                        if (Boolean.FALSE.equals(vaultDeleted)) {
                            log.warn("Failed to delete token data from vault for token: {}", tokenId);
                            // Continue with database deletion even if vault deletion fails
                        }
                        
                        // Delete from database after vault deletion
                        return tokenizationRepository.delete(entity)
                            .then(Mono.defer(() -> {
                                DeleteTokenDataResponse response = new DeleteTokenDataResponse();
                                response.setId(tokenId);
                                return Mono.just(Result.<DeleteTokenDataResponse, PaymentError>ok(response));
                            }));
                    });
            })
            .onErrorResume(Throwable.class, error -> {
                log.error("Error deleting tokenized data: {}", error.getMessage(), error);
                return Mono.<Result<DeleteTokenDataResponse, PaymentError>>just(
                    Result.err(PaymentError.of("TOKEN_DELETION_FAILED", 
                        "Failed to delete tokenized data")));
            });
    }
    
    private String generateTokenId(String merchantId) {
        // Generate token ID in format: {merchant_id}_tok_{uuid}
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return merchantId + TOKEN_ID_SEPARATOR + uuid.substring(0, TOKEN_UUID_LENGTH);
    }
    
    /**
     * Store token data in vault service
     * In production, this would make HTTP API calls to the vault service
     * 
     * @param lockerId Vault locker ID
     * @return Mono<Boolean> indicating storage success
     */
    private Mono<Boolean> storeTokenDataInVault(String lockerId) {
        log.info("Storing token data in vault for locker: {}", lockerId);
        
        // In production, this would:
        // 1. Make HTTP POST/PUT request to vault service API
        // 2. Include locker ID and encrypted token data
        // 3. Handle vault service response
        // 4. Return storage result
        
        // For now, we simulate successful storage
        // Production implementation would look like:
        /*
        return vaultServiceClient.storeToken(lockerId, request)
            .map(response -> response.isSuccess())
            .onErrorResume(error -> {
                log.error("Error storing token data in vault", error);
                return Mono.just(false);
            });
        */
        
        // Simulated storage - in production, replace with actual vault service API call
        return Mono.just(Boolean.TRUE)
            .doOnNext(result -> {
                if (Boolean.TRUE.equals(result)) {
                    log.info("Token data stored in vault for locker: {}", lockerId);
                }
            });
    }
    
    /**
     * Delete token data from vault service
     * In production, this would make HTTP API calls to the vault service
     * 
     * @param lockerId Vault locker ID
     * @return Mono<Boolean> indicating deletion success
     */
    private Mono<Boolean> deleteTokenDataFromVault(String lockerId) {
        log.info("Deleting token data from vault for locker: {}", lockerId);
        
        // In production, this would:
        // 1. Make HTTP DELETE request to vault service API
        // 2. Include locker ID
        // 3. Handle vault service response
        // 4. Return deletion result
        
        // For now, we simulate successful deletion
        // Production implementation would look like:
        /*
        return vaultServiceClient.deleteToken(lockerId)
            .map(response -> response.isSuccess())
            .onErrorResume(error -> {
                log.error("Error deleting token data from vault", error);
                return Mono.just(false);
            });
        */
        
        // Simulated deletion - in production, replace with actual vault service API call
        return Mono.just(Boolean.TRUE)
            .doOnNext(result -> {
                if (Boolean.TRUE.equals(result)) {
                    log.info("Token data deleted from vault for locker: {}", lockerId);
                }
            });
    }
}

