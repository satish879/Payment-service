package com.hyperswitch.storage.repository;

import com.hyperswitch.storage.entity.SuccessRateWindowEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Repository for success rate window entities
 */
@Repository
public interface SuccessRateWindowRepository extends ReactiveCrudRepository<SuccessRateWindowEntity, String> {
    
    /**
     * Find windows for a profile and connector within a time range
     */
    @Query("SELECT * FROM success_rate_window WHERE profile_id = :profileId AND connector = :connector " +
           "AND window_start >= :startTime AND window_end <= :endTime ORDER BY window_start DESC")
    Flux<SuccessRateWindowEntity> findByProfileIdAndConnectorAndTimeRange(
        String profileId, 
        String connector, 
        Instant startTime, 
        Instant endTime
    );
    
    /**
     * Find the latest window for a profile and connector
     */
    @Query("SELECT * FROM success_rate_window WHERE profile_id = :profileId AND connector = :connector " +
           "ORDER BY window_end DESC LIMIT 1")
    Mono<SuccessRateWindowEntity> findLatestByProfileIdAndConnector(String profileId, String connector);
    
    /**
     * Find windows by payment method and currency
     */
    @Query("SELECT * FROM success_rate_window WHERE profile_id = :profileId AND connector = :connector " +
           "AND payment_method = :paymentMethod AND currency = :currency " +
           "AND window_start >= :startTime AND window_end <= :endTime ORDER BY window_start DESC")
    Flux<SuccessRateWindowEntity> findByProfileIdAndConnectorAndPaymentMethodAndCurrencyAndTimeRange(
        String profileId,
        String connector,
        String paymentMethod,
        String currency,
        Instant startTime,
        Instant endTime
    );
}

