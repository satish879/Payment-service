package com.hyperswitch.core.apikeys.impl;

import com.hyperswitch.common.dto.ApiKeyRequest;
import com.hyperswitch.common.dto.ApiKeyResponse;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
import com.hyperswitch.core.apikeys.ApiKeyService;
import com.hyperswitch.storage.entity.ApiKeyEntity;
import com.hyperswitch.storage.repository.ApiKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Implementation of ApiKeyService
 */
@Service
public class ApiKeyServiceImpl implements ApiKeyService {
    
    private static final Logger log = LoggerFactory.getLogger(ApiKeyServiceImpl.class);
    
    @Value("${hyperswitch.api-key.prefix:snd_}")
    private String apiKeyPrefix;
    
    @Value("${hyperswitch.api-key.hash-key:default-hash-key-change-in-production}")
    private String hashKey;
    
    private final ApiKeyRepository apiKeyRepository;
    
    @Autowired
    public ApiKeyServiceImpl(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }
    
    @Override
    public Mono<Result<ApiKeyResponse, PaymentError>> createApiKey(
            String merchantId, 
            ApiKeyRequest request) {
        
        log.info("Creating API key for merchant: {}, name: {}", merchantId, request.getName());
        
        String keyId = generateKeyId(merchantId);
        String plaintextApiKey = generateApiKey(keyId);
        String hashedApiKey = hashApiKey(plaintextApiKey);
        
        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setKeyId(keyId);
        entity.setMerchantId(merchantId);
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setHashKey(hashKey);
        entity.setHashedApiKey(hashedApiKey);
        entity.setPrefix(apiKeyPrefix);
        entity.setCreatedAt(Instant.now());
        entity.setExpiresAt(request.getExpiresAt());
        entity.setLastUsed(null);
        
        return apiKeyRepository.save(entity)
            .map(saved -> {
                ApiKeyResponse response = toApiKeyResponse(saved);
                response.setApiKey(plaintextApiKey); // Only set on creation
                return response;
            })
            .map(Result::<ApiKeyResponse, PaymentError>ok)
            .onErrorResume(error -> {
                log.error("Error creating API key: {}", error.getMessage(), error);
                return Mono.just(Result.err(PaymentError.of("API_KEY_CREATE_FAILED",
                    "Failed to create API key: " + error.getMessage())));
            });
    }
    
    @Override
    public Mono<Result<Flux<ApiKeyResponse>, PaymentError>> listApiKeys(String merchantId) {
        log.info("Listing API keys for merchant: {}", merchantId);
        
        return Mono.just(Result.<Flux<ApiKeyResponse>, PaymentError>ok(apiKeyRepository.findByMerchantId(merchantId)
            .map(this::toApiKeyResponse)))
            .onErrorResume(error -> {
                log.error("Error listing API keys: {}", error.getMessage(), error);
                return Mono.just(Result.err(PaymentError.of("API_KEY_LIST_FAILED",
                    "Failed to list API keys: " + error.getMessage())));
            });
    }
    
    @Override
    public Mono<Result<ApiKeyResponse, PaymentError>> getApiKey(
            String merchantId, 
            String keyId) {
        
        log.info("Getting API key: {} for merchant: {}", keyId, merchantId);
        
        return apiKeyRepository.findByKeyIdAndMerchantId(keyId, merchantId)
            .map(this::toApiKeyResponse)
            .map(Result::<ApiKeyResponse, PaymentError>ok)
            .switchIfEmpty(Mono.just(Result.err(PaymentError.of("API_KEY_NOT_FOUND",
                "API key not found: " + keyId))))
            .onErrorResume(error -> {
                log.error("Error getting API key: {}", error.getMessage(), error);
                return Mono.just(Result.err(PaymentError.of("API_KEY_RETRIEVAL_FAILED",
                    "Failed to get API key: " + error.getMessage())));
            });
    }
    
    @Override
    public Mono<Result<ApiKeyResponse, PaymentError>> updateApiKey(
            String merchantId, 
            String keyId,
            ApiKeyRequest request) {
        
        log.info("Updating API key: {} for merchant: {}", keyId, merchantId);
        
        return apiKeyRepository.findByKeyIdAndMerchantId(keyId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException("API_KEY_NOT_FOUND")))
            .flatMap(entity -> {
                if (request.getName() != null) {
                    entity.setName(request.getName());
                }
                if (request.getDescription() != null) {
                    entity.setDescription(request.getDescription());
                }
                if (request.getExpiresAt() != null) {
                    entity.setExpiresAt(request.getExpiresAt());
                }
                
                return apiKeyRepository.save(entity)
                    .map(this::toApiKeyResponse)
                    .map(Result::<ApiKeyResponse, PaymentError>ok);
            })
            .onErrorResume(error -> {
                log.error("Error updating API key: {}", error.getMessage(), error);
                if (error.getMessage() != null && error.getMessage().contains("API_KEY_NOT_FOUND")) {
                    return Mono.just(Result.<ApiKeyResponse, PaymentError>err(
                        PaymentError.of("API_KEY_NOT_FOUND", "API key not found: " + keyId)));
                }
                return Mono.just(Result.err(PaymentError.of("API_KEY_UPDATE_FAILED",
                    "Failed to update API key: " + error.getMessage())));
            });
    }
    
    @Override
    public Mono<Result<Void, PaymentError>> revokeApiKey(
            String merchantId, 
            String keyId) {
        
        log.info("Revoking API key: {} for merchant: {}", keyId, merchantId);
        
        return apiKeyRepository.findByKeyIdAndMerchantId(keyId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException("API_KEY_NOT_FOUND")))
            .flatMap(entity -> apiKeyRepository.delete(entity)
                .thenReturn(Result.<Void, PaymentError>ok(null)))
            .onErrorResume(error -> {
                log.error("Error revoking API key: {}", error.getMessage(), error);
                if (error.getMessage() != null && error.getMessage().contains("API_KEY_NOT_FOUND")) {
                    return Mono.just(Result.<Void, PaymentError>err(
                        PaymentError.of("API_KEY_NOT_FOUND", "API key not found: " + keyId)));
                }
                return Mono.just(Result.err(PaymentError.of("API_KEY_REVOKE_FAILED",
                    "Failed to revoke API key: " + error.getMessage())));
            });
    }
    
    private ApiKeyResponse toApiKeyResponse(ApiKeyEntity entity) {
        ApiKeyResponse response = new ApiKeyResponse();
        response.setKeyId(entity.getKeyId());
        response.setMerchantId(entity.getMerchantId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setPrefix(entity.getPrefix());
        response.setCreatedAt(entity.getCreatedAt());
        response.setExpiresAt(entity.getExpiresAt());
        response.setLastUsed(entity.getLastUsed());
        // Note: apiKey is only set on creation, not on retrieval
        return response;
    }
    
    private String generateKeyId(String merchantId) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return merchantId + "_key_" + uuid.substring(0, 32);
    }
    
    private String generateApiKey(String keyId) {
        return apiKeyPrefix + keyId;
    }
    
    private String hashApiKey(String plaintextApiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((plaintextApiKey + hashKey).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            log.error("Error hashing API key: SHA-256 algorithm not available", e);
            throw new IllegalStateException("Failed to hash API key: SHA-256 algorithm not available", e);
        }
    }
}

