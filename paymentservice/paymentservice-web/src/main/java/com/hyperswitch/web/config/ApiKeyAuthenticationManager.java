package com.hyperswitch.web.config;

import com.hyperswitch.storage.repository.ApiKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * Authentication manager for API key validation
 * Validates API keys against database
 */
public class ApiKeyAuthenticationManager implements ReactiveAuthenticationManager {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationManager.class);
    private static final List<SimpleGrantedAuthority> AUTHORITIES = 
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_USER"));
    
    private final ApiKeyRepository apiKeyRepository;
    
    @Value("${hyperswitch.api-key.hash-key:default-hash-key-change-in-production}")
    private String hashKey;
    
    public ApiKeyAuthenticationManager(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return Mono.empty();
        }
        
        String apiKey = authentication.getPrincipal().toString();
        if (apiKey == null || apiKey.isEmpty()) {
            return Mono.empty();
        }
        
        // If repository is not available, fall back to basic validation
        if (apiKeyRepository == null) {
            log.warn("ApiKeyRepository not available - using basic API key validation");
            // Basic validation: accept any non-empty API key
            Authentication authenticated = new UsernamePasswordAuthenticationToken(
                apiKey,
                null,
                AUTHORITIES
            );
            return Mono.just(authenticated);
        }
        
        // Extract key ID from API key (format: prefix_keyId)
        String keyId = extractKeyIdFromApiKey(apiKey);
        if (keyId == null) {
            log.warn("Invalid API key format");
            return Mono.empty();
        }
        
        // Validate API key against database
        return validateApiKey(apiKey, keyId)
            .flatMap(valid -> {
                if (Boolean.TRUE.equals(valid)) {
                    // Update last used timestamp
                    updateLastUsed(keyId).subscribe();
                    
                    // Create authenticated token
                    Authentication authenticated = new UsernamePasswordAuthenticationToken(
                        apiKey,
                        null,
                        AUTHORITIES
                    );
                    return Mono.just(authenticated);
                }
                return Mono.<Authentication>empty();
            })
            .switchIfEmpty(Mono.empty())
            .onErrorResume(error -> {
                log.error("Error authenticating API key: {}", error.getMessage(), error);
                return Mono.empty();
            });
    }
    
    /**
     * Extract key ID from API key
     * API key format: prefix_keyId
     */
    private String extractKeyIdFromApiKey(String apiKey) {
        // Find the first underscore and extract everything after it
        int underscoreIndex = apiKey.indexOf('_');
        if (underscoreIndex < 0 || underscoreIndex >= apiKey.length() - 1) {
            return null;
        }
        return apiKey.substring(underscoreIndex + 1);
    }
    
    /**
     * Validate API key against database
     */
    private Mono<Boolean> validateApiKey(String plaintextApiKey, String keyId) {
        // Hash the provided API key
        String hashedApiKey = hashApiKey(plaintextApiKey);
        
        // Find API key in database by keyId
        return apiKeyRepository.findByKeyId(keyId)
            .map(entity -> {
                // Check if key is expired
                if (entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(Instant.now())) {
                    log.warn("API key expired: {}", keyId);
                    return Boolean.FALSE;
                }
                
                // Compare hashed API keys
                boolean isValid = hashedApiKey.equals(entity.getHashedApiKey());
                if (!isValid) {
                    log.warn("API key validation failed: {}", keyId);
                }
                return isValid;
            })
            .switchIfEmpty(Mono.just(Boolean.FALSE))
            .onErrorResume(error -> {
                log.error("Error validating API key: {}", error.getMessage(), error);
                return Mono.just(Boolean.FALSE);
            });
    }
    
    /**
     * Hash API key using SHA-256
     */
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
    
    /**
     * Update last used timestamp for API key
     */
    private Mono<Void> updateLastUsed(String keyId) {
        if (apiKeyRepository == null) {
            return Mono.empty();
        }
        
        return apiKeyRepository.findByKeyId(keyId)
            .flatMap(entity -> {
                entity.setLastUsed(Instant.now());
                return apiKeyRepository.save(entity);
            })
            .then()
            .onErrorResume(error -> {
                log.warn("Error updating last used timestamp for API key: {}", keyId, error);
                return Mono.empty();
            });
    }
}

