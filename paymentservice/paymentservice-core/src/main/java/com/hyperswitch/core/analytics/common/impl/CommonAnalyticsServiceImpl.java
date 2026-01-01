package com.hyperswitch.core.analytics.common.impl;

import com.hyperswitch.common.analytics.AnalyticsService;
import com.hyperswitch.common.dto.ConnectorAnalyticsResponse;
import com.hyperswitch.common.dto.CustomerAnalyticsResponse;
import com.hyperswitch.common.dto.PaymentAnalyticsResponse;
import com.hyperswitch.common.dto.RevenueAnalyticsResponse;
import com.hyperswitch.common.enums.Connector;
import com.hyperswitch.storage.entity.ConnectorSuccessRateEntity;
import com.hyperswitch.storage.entity.SuccessRateWindowEntity;
import com.hyperswitch.storage.repository.ConnectorSuccessRateRepository;
import com.hyperswitch.storage.repository.SuccessRateWindowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Implementation of com.hyperswitch.common.analytics.AnalyticsService
 * Provides analytics tracking for payment success rates and performance metrics
 */
@Service
public class CommonAnalyticsServiceImpl implements AnalyticsService {
    
    private static final Logger log = LoggerFactory.getLogger(CommonAnalyticsServiceImpl.class);
    
    private final ConnectorSuccessRateRepository successRateRepository;
    private final SuccessRateWindowRepository windowRepository;
    
    @Autowired
    public CommonAnalyticsServiceImpl(
            ConnectorSuccessRateRepository successRateRepository,
            SuccessRateWindowRepository windowRepository) {
        this.successRateRepository = successRateRepository;
        this.windowRepository = windowRepository;
        log.info("CommonAnalyticsServiceImpl initialized");
    }
    
    @Override
    public Mono<Void> recordPaymentAttempt(
            String merchantId,
            String profileId,
            Connector connector,
            String paymentMethod,
            String currency,
            boolean success) {
        log.debug("Recording payment attempt: merchant={}, profile={}, connector={}, method={}, currency={}, success={}",
                merchantId, profileId, connector, paymentMethod, currency, success);
        
        String connectorName = connector != null ? connector.name() : "UNKNOWN";
        String pm = paymentMethod != null ? paymentMethod : "unknown";
        String curr = currency != null ? currency : "USD";
        
        return successRateRepository
            .findByMerchantIdAndConnectorAndProfileIdAndPaymentMethodAndCurrency(
                merchantId, connectorName, profileId, pm, curr)
            .switchIfEmpty(Mono.defer(() -> {
                // Create new entity if not found
                ConnectorSuccessRateEntity entity = new ConnectorSuccessRateEntity();
                entity.setId(UUID.randomUUID().toString());
                entity.setMerchantId(merchantId);
                entity.setProfileId(profileId);
                entity.setConnector(connectorName);
                entity.setPaymentMethod(pm);
                entity.setCurrency(curr);
                entity.setTotalAttempts(0L);
                entity.setSuccessfulAttempts(0L);
                entity.setFailedAttempts(0L);
                entity.setSuccessRate(BigDecimal.ZERO);
                entity.setCreatedAt(Instant.now());
                entity.setModifiedAt(Instant.now());
                return Mono.just(entity);
            }))
            .flatMap(entity -> {
                // Update counters
                entity.setTotalAttempts((entity.getTotalAttempts() != null ? entity.getTotalAttempts() : 0L) + 1);
                if (success) {
                    entity.setSuccessfulAttempts(
                        (entity.getSuccessfulAttempts() != null ? entity.getSuccessfulAttempts() : 0L) + 1);
                } else {
                    entity.setFailedAttempts(
                        (entity.getFailedAttempts() != null ? entity.getFailedAttempts() : 0L) + 1);
                }
                
                // Recalculate success rate
                if (entity.getTotalAttempts() > 0) {
                    BigDecimal rate = BigDecimal.valueOf(entity.getSuccessfulAttempts())
                        .divide(BigDecimal.valueOf(entity.getTotalAttempts()), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                    entity.setSuccessRate(rate);
                }
                
                entity.setLastCalculatedAt(Instant.now());
                entity.setModifiedAt(Instant.now());
                
                return successRateRepository.save(entity)
                    .doOnSuccess(e -> log.debug("Successfully recorded payment attempt"))
                    .doOnError(error -> log.warn("Failed to record payment attempt", error))
                    .then();
            })
            .onErrorResume(error -> {
                log.error("Error recording payment attempt", error);
                return Mono.empty(); // Don't fail the payment flow if analytics fails
            });
    }
    
    @Override
    public Mono<BigDecimal> getSuccessRate(
            String merchantId,
            String profileId,
            Connector connector,
            String paymentMethod,
            String currency) {
        log.debug("Getting success rate: merchant={}, profile={}, connector={}, method={}, currency={}",
                merchantId, profileId, connector, paymentMethod, currency);
        
        String connectorName = connector != null ? connector.name() : "UNKNOWN";
        String pm = paymentMethod != null ? paymentMethod : "unknown";
        String curr = currency != null ? currency : "USD";
        
        return successRateRepository
            .findByMerchantIdAndConnectorAndProfileIdAndPaymentMethodAndCurrency(
                merchantId, connectorName, profileId, pm, curr)
            .map(ConnectorSuccessRateEntity::getSuccessRate)
            .defaultIfEmpty(BigDecimal.ZERO)
            .onErrorResume(error -> {
                log.warn("Error getting success rate", error);
                return Mono.just(BigDecimal.ZERO);
            });
    }
    
    @Override
    public Mono<Void> recalculateSuccessRates(String merchantId, String profileId) {
        log.info("Recalculating success rates for merchant={}, profile={}", merchantId, profileId);
        
        return successRateRepository
            .findByMerchantIdAndProfileId(merchantId, profileId)
            .flatMap(entity -> {
                if (entity.getTotalAttempts() != null && entity.getTotalAttempts() > 0) {
                    BigDecimal rate = BigDecimal.valueOf(
                        entity.getSuccessfulAttempts() != null ? entity.getSuccessfulAttempts() : 0L)
                        .divide(BigDecimal.valueOf(entity.getTotalAttempts()), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                    entity.setSuccessRate(rate);
                    entity.setLastCalculatedAt(Instant.now());
                    entity.setModifiedAt(Instant.now());
                    return successRateRepository.save(entity);
                }
                return Mono.just(entity);
            })
            .then()
            .doOnSuccess(v -> log.info("Successfully recalculated success rates"))
            .doOnError(error -> log.error("Error recalculating success rates", error))
            .onErrorResume(error -> Mono.empty());
    }
    
    @Override
    public Mono<PaymentAnalyticsResponse> getPaymentAnalytics(
            String merchantId,
            Instant startDate,
            Instant endDate,
            String currency) {
        log.debug("Getting payment analytics: merchant={}, start={}, end={}, currency={}",
                merchantId, startDate, endDate, currency);
        
        // Return empty analytics for now - can be enhanced later
        PaymentAnalyticsResponse response = new PaymentAnalyticsResponse();
        response.setTotalPayments(0L);
        response.setSuccessfulPayments(0L);
        response.setFailedPayments(0L);
        response.setTotalVolume(0L);
        response.setCurrency(currency != null ? currency : "USD");
        
        return Mono.just(response);
    }
    
    @Override
    public Flux<ConnectorAnalyticsResponse> getConnectorAnalytics(
            String merchantId,
            Instant startDate,
            Instant endDate) {
        log.debug("Getting connector analytics: merchant={}, start={}, end={}",
                merchantId, startDate, endDate);
        
        // Return empty analytics for now - can be enhanced later
        return Flux.empty();
    }
    
    @Override
    public Mono<RevenueAnalyticsResponse> getRevenueAnalytics(
            String merchantId,
            Instant startDate,
            Instant endDate) {
        log.debug("Getting revenue analytics: merchant={}, start={}, end={}",
                merchantId, startDate, endDate);
        
        // Return empty analytics for now - can be enhanced later
        RevenueAnalyticsResponse response = new RevenueAnalyticsResponse();
        response.setTotalRevenue(0L);
        response.setNetRevenue(0L);
        
        return Mono.just(response);
    }
    
    @Override
    public Mono<CustomerAnalyticsResponse> getCustomerAnalytics(
            String merchantId,
            String customerId) {
        log.debug("Getting customer analytics: merchant={}, customer={}", merchantId, customerId);
        
        // Return empty analytics for now - can be enhanced later
        CustomerAnalyticsResponse response = new CustomerAnalyticsResponse();
        response.setCustomerId(customerId);
        response.setTotalPayments(0L);
        response.setTotalSpent(0L);
        
        return Mono.just(response);
    }
    
    @Override
    public Mono<Void> updateSuccessRateWindow(
            String profileId,
            String connector,
            String paymentMethod,
            String currency,
            boolean success,
            int windowDurationMinutes) {
        log.debug("Updating success rate window: profile={}, connector={}, method={}, currency={}, success={}, window={}m",
                profileId, connector, paymentMethod, currency, success, windowDurationMinutes);
        
        Instant now = Instant.now();
        Instant windowStart = now.minus(windowDurationMinutes, ChronoUnit.MINUTES);
        Instant windowEnd = now;
        
        String pm = paymentMethod != null ? paymentMethod : "unknown";
        String curr = currency != null ? currency : "USD";
        String conn = connector != null ? connector : "UNKNOWN";
        
        // Find or create window entity
        return windowRepository
            .findByProfileIdAndConnectorAndPaymentMethodAndCurrencyAndTimeRange(
                profileId, conn, pm, curr, windowStart, windowEnd)
            .next() // Get the first matching window
            .switchIfEmpty(Mono.defer(() -> {
                // Create new window if not found
                SuccessRateWindowEntity window = new SuccessRateWindowEntity();
                window.setId(UUID.randomUUID().toString());
                window.setProfileId(profileId);
                window.setConnector(conn);
                window.setPaymentMethod(pm);
                window.setCurrency(curr);
                window.setWindowStart(windowStart);
                window.setWindowEnd(windowEnd);
                window.setTotalAttempts(0L);
                window.setSuccessfulAttempts(0L);
                window.setFailedAttempts(0L);
                window.setSuccessRate(BigDecimal.ZERO);
                window.setCreatedAt(now);
                window.setModifiedAt(now);
                return Mono.just(window);
            }))
            .flatMap(window -> {
                // Update counters
                window.setTotalAttempts((window.getTotalAttempts() != null ? window.getTotalAttempts() : 0L) + 1);
                if (success) {
                    window.setSuccessfulAttempts(
                        (window.getSuccessfulAttempts() != null ? window.getSuccessfulAttempts() : 0L) + 1);
                } else {
                    window.setFailedAttempts(
                        (window.getFailedAttempts() != null ? window.getFailedAttempts() : 0L) + 1);
                }
                
                // Recalculate success rate
                if (window.getTotalAttempts() > 0) {
                    BigDecimal rate = BigDecimal.valueOf(window.getSuccessfulAttempts())
                        .divide(BigDecimal.valueOf(window.getTotalAttempts()), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                    window.setSuccessRate(rate);
                }
                
                window.setModifiedAt(now);
                
                return windowRepository.save(window)
                    .doOnSuccess(w -> log.debug("Successfully updated success rate window"))
                    .doOnError(error -> log.warn("Failed to update success rate window", error))
                    .then();
            })
            .onErrorResume(error -> {
                log.error("Error updating success rate window", error);
                return Mono.empty(); // Don't fail the payment flow if analytics fails
            });
    }
    
    @Override
    public Mono<BigDecimal> getWindowedSuccessRate(
            String profileId,
            String connector,
            String paymentMethod,
            String currency,
            int windowDurationMinutes) {
        log.debug("Getting windowed success rate: profile={}, connector={}, method={}, currency={}, window={}m",
                profileId, connector, paymentMethod, currency, windowDurationMinutes);
        
        Instant now = Instant.now();
        Instant windowStart = now.minus(windowDurationMinutes, ChronoUnit.MINUTES);
        Instant windowEnd = now;
        
        String pm = paymentMethod != null ? paymentMethod : "unknown";
        String curr = currency != null ? currency : "USD";
        String conn = connector != null ? connector : "UNKNOWN";
        
        return windowRepository
            .findByProfileIdAndConnectorAndPaymentMethodAndCurrencyAndTimeRange(
                profileId, conn, pm, curr, windowStart, windowEnd)
            .collectList()
            .map(windows -> {
                if (windows.isEmpty()) {
                    return BigDecimal.ZERO;
                }
                
                // Aggregate all windows
                long totalAttempts = windows.stream()
                    .mapToLong(w -> w.getTotalAttempts() != null ? w.getTotalAttempts() : 0L)
                    .sum();
                long successfulAttempts = windows.stream()
                    .mapToLong(w -> w.getSuccessfulAttempts() != null ? w.getSuccessfulAttempts() : 0L)
                    .sum();
                
                if (totalAttempts > 0) {
                    return BigDecimal.valueOf(successfulAttempts)
                        .divide(BigDecimal.valueOf(totalAttempts), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                }
                return BigDecimal.ZERO;
            })
            .defaultIfEmpty(BigDecimal.ZERO)
            .onErrorResume(error -> {
                log.warn("Error getting windowed success rate", error);
                return Mono.just(BigDecimal.ZERO);
            });
    }
}

