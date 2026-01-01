package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.PaymentMetricsRequest;
import com.hyperswitch.common.dto.PaymentMetricsResponse;
import com.hyperswitch.common.dto.PaymentIntentMetricsRequest;
import com.hyperswitch.common.dto.PaymentIntentMetricsResponse;
import com.hyperswitch.common.dto.RefundMetricsRequest;
import com.hyperswitch.common.dto.RefundMetricsResponse;
import com.hyperswitch.common.dto.RoutingMetricsRequest;
import com.hyperswitch.common.dto.RoutingMetricsResponse;
import com.hyperswitch.common.dto.AuthEventMetricsRequest;
import com.hyperswitch.common.dto.AuthEventMetricsResponse;
import com.hyperswitch.common.dto.SdkEventMetricsRequest;
import com.hyperswitch.common.dto.SdkEventMetricsResponse;
import com.hyperswitch.common.dto.ActivePaymentsMetricsRequest;
import com.hyperswitch.common.dto.ActivePaymentsMetricsResponse;
import com.hyperswitch.common.dto.FrmMetricsRequest;
import com.hyperswitch.common.dto.FrmMetricsResponse;
import com.hyperswitch.common.dto.DisputeMetricsRequest;
import com.hyperswitch.common.dto.DisputeMetricsResponse;
import com.hyperswitch.common.dto.ApiEventMetricsRequest;
import com.hyperswitch.common.dto.ApiEventMetricsResponse;
import com.hyperswitch.common.dto.SankeyRequest;
import com.hyperswitch.common.dto.SankeyResponse;
import com.hyperswitch.core.analytics.AnalyticsService;
import com.hyperswitch.web.controller.PaymentException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.Map;

/**
 * REST controller for analytics metrics operations
 */
@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics Metrics", description = "Analytics metrics operations")
public class AnalyticsMetricsController {
    
    private static final Logger log = LoggerFactory.getLogger(AnalyticsMetricsController.class);
    
    private AnalyticsService analyticsService;
    
    // Default constructor to allow bean creation even if dependencies are missing
    public AnalyticsMetricsController() {
        log.warn("AnalyticsMetricsController created without dependencies - services will be null");
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
     * Get payment metrics
     * POST /api/analytics/v1/metrics/payments
     */
    @PostMapping("/v1/metrics/payments")
    @Operation(
        summary = "Get payment metrics",
        description = "Retrieves payment metrics with filtering and grouping options"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getPaymentMetrics(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody PaymentMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getPaymentMetrics(merchantId != null ? merchantId : "default", request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get merchant payment metrics
     * POST /api/analytics/v1/merchant/metrics/payments
     */
    @PostMapping("/v1/merchant/metrics/payments")
    @Operation(
        summary = "Get merchant payment metrics",
        description = "Retrieves payment metrics for a specific merchant"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Merchant payment metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getMerchantPaymentMetrics(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody PaymentMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getMerchantPaymentMetrics(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get org payment metrics
     * POST /api/analytics/v1/org/metrics/payments
     */
    @PostMapping("/v1/org/metrics/payments")
    @Operation(
        summary = "Get org payment metrics",
        description = "Retrieves payment metrics for an organization"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Org payment metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getOrgPaymentMetrics(
            @RequestHeader("org_id") String orgId,
            @RequestBody PaymentMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getOrgPaymentMetrics(orgId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get profile payment metrics
     * POST /api/analytics/v1/profile/metrics/payments
     */
    @PostMapping("/v1/profile/metrics/payments")
    @Operation(
        summary = "Get profile payment metrics",
        description = "Retrieves payment metrics for a specific profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile payment metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getProfilePaymentMetrics(
            @RequestHeader("profile_id") String profileId,
            @RequestBody PaymentMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getProfilePaymentMetrics(profileId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get payment metrics (v2)
     * POST /api/analytics/v2/metrics/payments
     */
    @PostMapping("/v2/metrics/payments")
    @Operation(
        summary = "Get payment metrics (v2)",
        description = "Retrieves payment metrics using v2 API"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getPaymentMetricsV2(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody PaymentMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getPaymentMetrics(merchantId != null ? merchantId : "default", request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get merchant payment metrics (v2)
     * POST /api/analytics/v2/merchant/metrics/payments
     */
    @PostMapping("/v2/merchant/metrics/payments")
    @Operation(
        summary = "Get merchant payment metrics (v2)",
        description = "Retrieves payment metrics for a specific merchant using v2 API"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Merchant payment metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getMerchantPaymentMetricsV2(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody PaymentMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getMerchantPaymentMetrics(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get org payment metrics (v2)
     * POST /api/analytics/v2/org/metrics/payments
     */
    @PostMapping("/v2/org/metrics/payments")
    @Operation(
        summary = "Get org payment metrics (v2)",
        description = "Retrieves payment metrics for an organization using v2 API"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Org payment metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getOrgPaymentMetricsV2(
            @RequestHeader("org_id") String orgId,
            @RequestBody PaymentMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getOrgPaymentMetrics(orgId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get profile payment metrics (v2)
     * POST /api/analytics/v2/profile/metrics/payments
     */
    @PostMapping("/v2/profile/metrics/payments")
    @Operation(
        summary = "Get profile payment metrics (v2)",
        description = "Retrieves payment metrics for a specific profile using v2 API"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile payment metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getProfilePaymentMetricsV2(
            @RequestHeader("profile_id") String profileId,
            @RequestBody PaymentMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getProfilePaymentMetrics(profileId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get payment intent metrics
     * POST /api/analytics/v1/metrics/payment_intents
     */
    @PostMapping("/v1/metrics/payment_intents")
    @Operation(
        summary = "Get payment intent metrics",
        description = "Retrieves payment intent metrics with filtering and grouping options"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment intent metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentIntentMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getPaymentIntentMetrics(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody PaymentIntentMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getPaymentIntentMetrics(merchantId != null ? merchantId : "default", request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get merchant payment intent metrics
     * POST /api/analytics/v1/merchant/metrics/payment_intents
     */
    @PostMapping("/v1/merchant/metrics/payment_intents")
    @Operation(
        summary = "Get merchant payment intent metrics",
        description = "Retrieves payment intent metrics for a specific merchant"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Merchant payment intent metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentIntentMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getMerchantPaymentIntentMetrics(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody PaymentIntentMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getMerchantPaymentIntentMetrics(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get org payment intent metrics
     * POST /api/analytics/v1/org/metrics/payment_intents
     */
    @PostMapping("/v1/org/metrics/payment_intents")
    @Operation(
        summary = "Get org payment intent metrics",
        description = "Retrieves payment intent metrics for an organization"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Org payment intent metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentIntentMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getOrgPaymentIntentMetrics(
            @RequestHeader("org_id") String orgId,
            @RequestBody PaymentIntentMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getOrgPaymentIntentMetrics(orgId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get profile payment intent metrics
     * POST /api/analytics/v1/profile/metrics/payment_intents
     */
    @PostMapping("/v1/profile/metrics/payment_intents")
    @Operation(
        summary = "Get profile payment intent metrics",
        description = "Retrieves payment intent metrics for a specific profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile payment intent metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentIntentMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getProfilePaymentIntentMetrics(
            @RequestHeader("profile_id") String profileId,
            @RequestBody PaymentIntentMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getProfilePaymentIntentMetrics(profileId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get refund metrics
     * POST /api/analytics/v1/metrics/refunds
     */
    @PostMapping("/v1/metrics/refunds")
    @Operation(
        summary = "Get refund metrics",
        description = "Retrieves refund metrics with filtering and grouping options"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Refund metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = RefundMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getRefundMetrics(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody RefundMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getRefundMetrics(merchantId != null ? merchantId : "default", request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get merchant refund metrics
     * POST /api/analytics/v1/merchant/metrics/refunds
     */
    @PostMapping("/v1/merchant/metrics/refunds")
    @Operation(
        summary = "Get merchant refund metrics",
        description = "Retrieves refund metrics for a specific merchant"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Merchant refund metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = RefundMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getMerchantRefundMetrics(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody RefundMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getMerchantRefundMetrics(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get org refund metrics
     * POST /api/analytics/v1/org/metrics/refunds
     */
    @PostMapping("/v1/org/metrics/refunds")
    @Operation(
        summary = "Get org refund metrics",
        description = "Retrieves refund metrics for an organization"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Org refund metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = RefundMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getOrgRefundMetrics(
            @RequestHeader("org_id") String orgId,
            @RequestBody RefundMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getOrgRefundMetrics(orgId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get profile refund metrics
     * POST /api/analytics/v1/profile/metrics/refunds
     */
    @PostMapping("/v1/profile/metrics/refunds")
    @Operation(
        summary = "Get profile refund metrics",
        description = "Retrieves refund metrics for a specific profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile refund metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = RefundMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getProfileRefundMetrics(
            @RequestHeader("profile_id") String profileId,
            @RequestBody RefundMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getProfileRefundMetrics(profileId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get routing metrics
     * POST /api/analytics/v1/metrics/routing
     */
    @PostMapping("/v1/metrics/routing")
    @Operation(
        summary = "Get routing metrics",
        description = "Retrieves routing metrics with filtering and grouping options"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Routing metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = RoutingMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getRoutingMetrics(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody RoutingMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getRoutingMetrics(merchantId != null ? merchantId : "default", request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get merchant routing metrics
     * POST /api/analytics/v1/merchant/metrics/routing
     */
    @PostMapping("/v1/merchant/metrics/routing")
    @Operation(
        summary = "Get merchant routing metrics",
        description = "Retrieves routing metrics for a specific merchant"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Merchant routing metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = RoutingMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getMerchantRoutingMetrics(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody RoutingMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getMerchantRoutingMetrics(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get org routing metrics
     * POST /api/analytics/v1/org/metrics/routing
     */
    @PostMapping("/v1/org/metrics/routing")
    @Operation(
        summary = "Get org routing metrics",
        description = "Retrieves routing metrics for an organization"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Org routing metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = RoutingMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getOrgRoutingMetrics(
            @RequestHeader("org_id") String orgId,
            @RequestBody RoutingMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getOrgRoutingMetrics(orgId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get profile routing metrics
     * POST /api/analytics/v1/profile/metrics/routing
     */
    @PostMapping("/v1/profile/metrics/routing")
    @Operation(
        summary = "Get profile routing metrics",
        description = "Retrieves routing metrics for a specific profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile routing metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = RoutingMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getProfileRoutingMetrics(
            @RequestHeader("profile_id") String profileId,
            @RequestBody RoutingMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getProfileRoutingMetrics(profileId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get auth event metrics
     * POST /api/analytics/v1/metrics/auth_events
     */
    @PostMapping("/v1/metrics/auth_events")
    @Operation(
        summary = "Get auth event metrics",
        description = "Retrieves auth event metrics with filtering and grouping options"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Auth event metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = AuthEventMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getAuthEventMetrics(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody AuthEventMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getAuthEventMetrics(merchantId != null ? merchantId : "default", request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get merchant auth event metrics
     * POST /api/analytics/v1/merchant/metrics/auth_events
     */
    @PostMapping("/v1/merchant/metrics/auth_events")
    @Operation(
        summary = "Get merchant auth event metrics",
        description = "Retrieves auth event metrics for a specific merchant"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Merchant auth event metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = AuthEventMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getMerchantAuthEventMetrics(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody AuthEventMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getMerchantAuthEventMetrics(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get org auth event metrics
     * POST /api/analytics/v1/org/metrics/auth_events
     */
    @PostMapping("/v1/org/metrics/auth_events")
    @Operation(
        summary = "Get org auth event metrics",
        description = "Retrieves auth event metrics for an organization"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Org auth event metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = AuthEventMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getOrgAuthEventMetrics(
            @RequestHeader("org_id") String orgId,
            @RequestBody AuthEventMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getOrgAuthEventMetrics(orgId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get profile auth event metrics
     * POST /api/analytics/v1/profile/metrics/auth_events
     */
    @PostMapping("/v1/profile/metrics/auth_events")
    @Operation(
        summary = "Get profile auth event metrics",
        description = "Retrieves auth event metrics for a specific profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile auth event metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = AuthEventMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getProfileAuthEventMetrics(
            @RequestHeader("profile_id") String profileId,
            @RequestBody AuthEventMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getProfileAuthEventMetrics(profileId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get SDK event metrics
     * POST /api/analytics/v1/metrics/sdk_events
     */
    @PostMapping("/v1/metrics/sdk_events")
    @Operation(
        summary = "Get SDK event metrics",
        description = "Retrieves SDK event metrics with filtering and grouping options"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "SDK event metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = SdkEventMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getSdkEventMetrics(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody SdkEventMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getSdkEventMetrics(merchantId != null ? merchantId : "default", request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get active payments metrics
     * POST /api/analytics/v1/metrics/active_payments
     */
    @PostMapping("/v1/metrics/active_payments")
    @Operation(
        summary = "Get active payments metrics",
        description = "Retrieves active payments metrics (payments in progress)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Active payments metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = ActivePaymentsMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getActivePaymentsMetrics(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody ActivePaymentsMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getActivePaymentsMetrics(merchantId != null ? merchantId : "default", request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get FRM metrics
     * POST /api/analytics/v1/metrics/frm
     */
    @PostMapping("/v1/metrics/frm")
    @Operation(
        summary = "Get FRM (fraud) metrics",
        description = "Retrieves FRM metrics with filtering and grouping options"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "FRM metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = FrmMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getFrmMetrics(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody FrmMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getFrmMetrics(merchantId != null ? merchantId : "default", request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get dispute metrics
     * POST /api/analytics/v1/metrics/disputes
     */
    @PostMapping("/v1/metrics/disputes")
    @Operation(
        summary = "Get dispute metrics",
        description = "Retrieves dispute metrics with filtering and grouping options"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Dispute metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = DisputeMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getDisputeMetrics(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody DisputeMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getDisputeMetrics(merchantId != null ? merchantId : "default", request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get merchant dispute metrics
     * POST /api/analytics/v1/merchant/metrics/disputes
     */
    @PostMapping("/v1/merchant/metrics/disputes")
    @Operation(
        summary = "Get merchant dispute metrics",
        description = "Retrieves dispute metrics for a specific merchant"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Merchant dispute metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = DisputeMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getMerchantDisputeMetrics(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody DisputeMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getMerchantDisputeMetrics(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get org dispute metrics
     * POST /api/analytics/v1/org/metrics/disputes
     */
    @PostMapping("/v1/org/metrics/disputes")
    @Operation(
        summary = "Get org dispute metrics",
        description = "Retrieves dispute metrics for an organization"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Org dispute metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = DisputeMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getOrgDisputeMetrics(
            @RequestHeader("org_id") String orgId,
            @RequestBody DisputeMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getOrgDisputeMetrics(orgId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get profile dispute metrics
     * POST /api/analytics/v1/profile/metrics/disputes
     */
    @PostMapping("/v1/profile/metrics/disputes")
    @Operation(
        summary = "Get profile dispute metrics",
        description = "Retrieves dispute metrics for a specific profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile dispute metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = DisputeMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getProfileDisputeMetrics(
            @RequestHeader("profile_id") String profileId,
            @RequestBody DisputeMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getProfileDisputeMetrics(profileId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get API event metrics
     * POST /api/analytics/v1/metrics/api_events
     */
    @PostMapping("/v1/metrics/api_events")
    @Operation(
        summary = "Get API event metrics",
        description = "Retrieves API event metrics with filtering and grouping options"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "API event metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = ApiEventMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getApiEventMetrics(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody ApiEventMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getApiEventMetrics(merchantId != null ? merchantId : "default", request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get merchant API event metrics
     * POST /api/analytics/v1/merchant/metrics/api_events
     */
    @PostMapping("/v1/merchant/metrics/api_events")
    @Operation(
        summary = "Get merchant API event metrics",
        description = "Retrieves API event metrics for a specific merchant"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Merchant API event metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = ApiEventMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getMerchantApiEventMetrics(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody ApiEventMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getMerchantApiEventMetrics(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get org API event metrics
     * POST /api/analytics/v1/org/metrics/api_events
     */
    @PostMapping("/v1/org/metrics/api_events")
    @Operation(
        summary = "Get org API event metrics",
        description = "Retrieves API event metrics for an organization"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Org API event metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = ApiEventMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getOrgApiEventMetrics(
            @RequestHeader("org_id") String orgId,
            @RequestBody ApiEventMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getOrgApiEventMetrics(orgId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get profile API event metrics
     * POST /api/analytics/v1/profile/metrics/api_events
     */
    @PostMapping("/v1/profile/metrics/api_events")
    @Operation(
        summary = "Get profile API event metrics",
        description = "Retrieves API event metrics for a specific profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile API event metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = ApiEventMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getProfileApiEventMetrics(
            @RequestHeader("profile_id") String profileId,
            @RequestBody ApiEventMetricsRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getProfileApiEventMetrics(profileId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get payment sankey diagram
     * POST /api/analytics/v1/metrics/sankey
     */
    @PostMapping("/v1/metrics/sankey")
    @Operation(
        summary = "Get payment sankey diagram",
        description = "Retrieves payment flow sankey diagram data"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment sankey diagram retrieved successfully",
            content = @Content(schema = @Schema(implementation = SankeyResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getSankey(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody SankeyRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getSankey(merchantId != null ? merchantId : "default", request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get merchant payment sankey diagram
     * POST /api/analytics/v1/merchant/metrics/sankey
     */
    @PostMapping("/v1/merchant/metrics/sankey")
    @Operation(
        summary = "Get merchant payment sankey diagram",
        description = "Retrieves payment flow sankey diagram data for a specific merchant"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Merchant payment sankey diagram retrieved successfully",
            content = @Content(schema = @Schema(implementation = SankeyResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getMerchantSankey(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody SankeyRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getMerchantSankey(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get org payment sankey diagram
     * POST /api/analytics/v1/org/metrics/sankey
     */
    @PostMapping("/v1/org/metrics/sankey")
    @Operation(
        summary = "Get org payment sankey diagram",
        description = "Retrieves payment flow sankey diagram data for an organization"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Org payment sankey diagram retrieved successfully",
            content = @Content(schema = @Schema(implementation = SankeyResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getOrgSankey(
            @RequestHeader("org_id") String orgId,
            @RequestBody SankeyRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getOrgSankey(orgId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get profile payment sankey diagram
     * POST /api/analytics/v1/profile/metrics/sankey
     */
    @PostMapping("/v1/profile/metrics/sankey")
    @Operation(
        summary = "Get profile payment sankey diagram",
        description = "Retrieves payment flow sankey diagram data for a specific profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile payment sankey diagram retrieved successfully",
            content = @Content(schema = @Schema(implementation = SankeyResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getProfileSankey(
            @RequestHeader("profile_id") String profileId,
            @RequestBody SankeyRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getProfileSankey(profileId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get auth event sankey diagram
     * POST /api/analytics/v1/metrics/auth_events/sankey
     */
    @PostMapping("/v1/metrics/auth_events/sankey")
    @Operation(
        summary = "Get auth event sankey diagram",
        description = "Retrieves authentication event flow sankey diagram data"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Auth event sankey diagram retrieved successfully",
            content = @Content(schema = @Schema(implementation = SankeyResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getAuthEventSankey(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody SankeyRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getAuthEventSankey(merchantId != null ? merchantId : "default", request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get merchant auth event sankey diagram
     * POST /api/analytics/v1/merchant/metrics/auth_events/sankey
     */
    @PostMapping("/v1/merchant/metrics/auth_events/sankey")
    @Operation(
        summary = "Get merchant auth event sankey diagram",
        description = "Retrieves authentication event flow sankey diagram data for a specific merchant"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Merchant auth event sankey diagram retrieved successfully",
            content = @Content(schema = @Schema(implementation = SankeyResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getMerchantAuthEventSankey(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody SankeyRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getMerchantAuthEventSankey(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get org auth event sankey diagram
     * POST /api/analytics/v1/org/metrics/auth_events/sankey
     */
    @PostMapping("/v1/org/metrics/auth_events/sankey")
    @Operation(
        summary = "Get org auth event sankey diagram",
        description = "Retrieves authentication event flow sankey diagram data for an organization"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Org auth event sankey diagram retrieved successfully",
            content = @Content(schema = @Schema(implementation = SankeyResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getOrgAuthEventSankey(
            @RequestHeader("org_id") String orgId,
            @RequestBody SankeyRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getOrgAuthEventSankey(orgId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get profile auth event sankey diagram
     * POST /api/analytics/v1/profile/metrics/auth_events/sankey
     */
    @PostMapping("/v1/profile/metrics/auth_events/sankey")
    @Operation(
        summary = "Get profile auth event sankey diagram",
        description = "Retrieves authentication event flow sankey diagram data for a specific profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile auth event sankey diagram retrieved successfully",
            content = @Content(schema = @Schema(implementation = SankeyResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> getProfileAuthEventSankey(
            @RequestHeader("profile_id") String profileId,
            @RequestBody SankeyRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.getProfileAuthEventSankey(profileId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
}

