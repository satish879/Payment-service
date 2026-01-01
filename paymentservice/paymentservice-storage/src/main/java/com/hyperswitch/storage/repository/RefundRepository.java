package com.hyperswitch.storage.repository;

import com.hyperswitch.storage.entity.RefundEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface RefundRepository extends ReactiveCrudRepository<RefundEntity, String> {
    
    Mono<RefundEntity> findByRefundIdAndMerchantId(String refundId, String merchantId);
    
    Flux<RefundEntity> findByPaymentIdAndMerchantId(String paymentId, String merchantId);
    
    Mono<RefundEntity> findByPaymentIdAndMerchantIdAndRefundId(
        String paymentId, 
        String merchantId, 
        String refundId
    );
    
    Flux<RefundEntity> findByMerchantIdOrderByCreatedAtDesc(String merchantId);
    
    Flux<RefundEntity> findByMerchantIdAndRefundStatusOrderByCreatedAtDesc(String merchantId, String refundStatus);
    
    Flux<RefundEntity> findByMerchantIdAndConnectorOrderByCreatedAtDesc(String merchantId, String connector);
    
    Flux<RefundEntity> findByMerchantIdAndCreatedAtBetween(
        String merchantId,
        java.time.Instant startTime,
        java.time.Instant endTime
    );
}

