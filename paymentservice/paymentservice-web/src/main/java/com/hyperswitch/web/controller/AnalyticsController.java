package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.ConnectorAnalyticsResponse;
import com.hyperswitch.common.dto.CustomerAnalyticsResponse;
import com.hyperswitch.common.dto.PaymentAnalyticsResponse;
import com.hyperswitch.common.dto.RevenueAnalyticsResponse;
import com.hyperswitch.common.analytics.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * REST controller for analytics
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private AnalyticsService analyticsService;

    // Default constructor to allow bean creation even if dependencies are missing
    public AnalyticsController() {
        log.warn("AnalyticsController created without dependencies - services will be null");
    }

    @Autowired(required = false)
    public void setAnalyticsService(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    private Mono<ResponseEntity<?>> checkServiceAvailable() {
        if (analyticsService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Analytics service is not available")));
        }
        return null;
    }

    /**
     * Get payment analytics
     */
    @GetMapping("/payments")
    public Mono<ResponseEntity<?>> getPaymentAnalytics(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) String currency) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getPaymentAnalytics(merchantId, startDate, endDate, currency)
            .map(ResponseEntity::ok);
    }

    /**
     * Get connector analytics
     */
    @GetMapping("/connectors")
    public Mono<ResponseEntity<?>> getConnectorAnalytics(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        Flux<ConnectorAnalyticsResponse> analytics = analyticsService.getConnectorAnalytics(merchantId, startDate, endDate);
        return Mono.just(ResponseEntity.ok(analytics));
    }

    /**
     * Get revenue analytics
     */
    @GetMapping("/revenue")
    public Mono<ResponseEntity<?>> getRevenueAnalytics(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getRevenueAnalytics(merchantId, startDate, endDate)
            .map(ResponseEntity::ok);
    }

    /**
     * Get customer analytics
     */
    @GetMapping("/customers/{customerId}")
    public Mono<ResponseEntity<?>> getCustomerAnalytics(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @PathVariable String customerId) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getCustomerAnalytics(merchantId, customerId)
            .map(ResponseEntity::ok);
    }
}

