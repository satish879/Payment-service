package com.hyperswitch.storage.repository;

import com.hyperswitch.storage.entity.ApiKeyEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for ApiKey entity
 */
@Repository
public interface ApiKeyRepository extends ReactiveCrudRepository<ApiKeyEntity, String> {
    
    @Query("SELECT * FROM api_keys WHERE merchant_id = :merchantId")
    Flux<ApiKeyEntity> findByMerchantId(String merchantId);
    
    @Query("SELECT * FROM api_keys WHERE key_id = :keyId AND merchant_id = :merchantId")
    Mono<ApiKeyEntity> findByKeyIdAndMerchantId(String keyId, String merchantId);
    
    @Query("SELECT * FROM api_keys WHERE prefix = :prefix")
    Mono<ApiKeyEntity> findByPrefix(String prefix);
    
    @Query("SELECT * FROM api_keys WHERE key_id = :keyId")
    Mono<ApiKeyEntity> findByKeyId(String keyId);
}

