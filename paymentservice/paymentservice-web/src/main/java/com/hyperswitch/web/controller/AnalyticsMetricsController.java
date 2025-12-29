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
import com.hyperswitch.core.analytics.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST controller for analytics metrics operations
 */
@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics Metrics", description = "Analytics metrics operations")
public class AnalyticsMetricsController {
    
    private final AnalyticsService analyticsService;
    
    @Autowired
    public AnalyticsMetricsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
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
    public Mono<ResponseEntity<PaymentMetricsResponse>> getPaymentMetrics(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody PaymentMetricsRequest request) {
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
    public Mono<ResponseEntity<PaymentMetricsResponse>> getMerchantPaymentMetrics(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody PaymentMetricsRequest request) {
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
    public Mono<ResponseEntity<PaymentMetricsResponse>> getOrgPaymentMetrics(
            @RequestHeader("org_id") String orgId,
            @RequestBody PaymentMetricsRequest request) {
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
    public Mono<ResponseEntity<PaymentMetricsResponse>> getProfilePaymentMetrics(
            @RequestHeader("profile_id") String profileId,
            @RequestBody PaymentMetricsRequest request) {
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
    public Mono<ResponseEntity<PaymentMetricsResponse>> getPaymentMetricsV2(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody PaymentMetricsRequest request) {
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
    public Mono<ResponseEntity<PaymentMetricsResponse>> getMerchantPaymentMetricsV2(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody PaymentMetricsRequest request) {
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
    public Mono<ResponseEntity<PaymentMetricsResponse>> getOrgPaymentMetricsV2(
            @RequestHeader("org_id") String orgId,
            @RequestBody PaymentMetricsRequest request) {
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
    public Mono<ResponseEntity<PaymentMetricsResponse>> getProfilePaymentMetricsV2(
            @RequestHeader("profile_id") String profileId,
            @RequestBody PaymentMetricsRequest request) {
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
    public Mono<ResponseEntity<PaymentIntentMetricsResponse>> getPaymentIntentMetrics(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody PaymentIntentMetricsRequest request) {
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
    public Mono<ResponseEntity<PaymentIntentMetricsResponse>> getMerchantPaymentIntentMetrics(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody PaymentIntentMetricsRequest request) {
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
    public Mono<ResponseEntity<PaymentIntentMetricsResponse>> getOrgPaymentIntentMetrics(
            @RequestHeader("org_id") String orgId,
            @RequestBody PaymentIntentMetricsRequest request) {
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
    public Mono<ResponseEntity<PaymentIntentMetricsResponse>> getProfilePaymentIntentMetrics(
            @RequestHeader("profile_id") String profileId,
            @RequestBody PaymentIntentMetricsRequest request) {
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
    public Mono<ResponseEntity<RefundMetricsResponse>> getRefundMetrics(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody RefundMetricsRequest request) {
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
    public Mono<ResponseEntity<RefundMetricsResponse>> getMerchantRefundMetrics(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody RefundMetricsRequest request) {
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
    public Mono<ResponseEntity<RefundMetricsResponse>> getOrgRefundMetrics(
            @RequestHeader("org_id") String orgId,
            @RequestBody RefundMetricsRequest request) {
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
    public Mono<ResponseEntity<RefundMetricsResponse>> getProfileRefundMetrics(
            @RequestHeader("profile_id") String profileId,
            @RequestBody RefundMetricsRequest request) {
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
    public Mono<ResponseEntity<RoutingMetricsResponse>> getRoutingMetrics(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody RoutingMetricsRequest request) {
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
    public Mono<ResponseEntity<RoutingMetricsResponse>> getMerchantRoutingMetrics(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody RoutingMetricsRequest request) {
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
    public Mono<ResponseEntity<RoutingMetricsResponse>> getOrgRoutingMetrics(
            @RequestHeader("org_id") String orgId,
            @RequestBody RoutingMetricsRequest request) {
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
    public Mono<ResponseEntity<RoutingMetricsResponse>> getProfileRoutingMetrics(
            @RequestHeader("profile_id") String profileId,
            @RequestBody RoutingMetricsRequest request) {
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
    public Mono<ResponseEntity<AuthEventMetricsResponse>> getAuthEventMetrics(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody AuthEventMetricsRequest request) {
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
    public Mono<ResponseEntity<AuthEventMetricsResponse>> getMerchantAuthEventMetrics(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody AuthEventMetricsRequest request) {
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
    public Mono<ResponseEntity<AuthEventMetricsResponse>> getOrgAuthEventMetrics(
            @RequestHeader("org_id") String orgId,
            @RequestBody AuthEventMetricsRequest request) {
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
    public Mono<ResponseEntity<AuthEventMetricsResponse>> getProfileAuthEventMetrics(
            @RequestHeader("profile_id") String profileId,
            @RequestBody AuthEventMetricsRequest request) {
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
    public Mono<ResponseEntity<SdkEventMetricsResponse>> getSdkEventMetrics(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody SdkEventMetricsRequest request) {
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
    public Mono<ResponseEntity<ActivePaymentsMetricsResponse>> getActivePaymentsMetrics(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody ActivePaymentsMetricsRequest request) {
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
    public Mono<ResponseEntity<FrmMetricsResponse>> getFrmMetrics(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody FrmMetricsRequest request) {
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
    public Mono<ResponseEntity<DisputeMetricsResponse>> getDisputeMetrics(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody DisputeMetricsRequest request) {
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
    public Mono<ResponseEntity<DisputeMetricsResponse>> getMerchantDisputeMetrics(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody DisputeMetricsRequest request) {
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
    public Mono<ResponseEntity<DisputeMetricsResponse>> getOrgDisputeMetrics(
            @RequestHeader("org_id") String orgId,
            @RequestBody DisputeMetricsRequest request) {
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
    public Mono<ResponseEntity<DisputeMetricsResponse>> getProfileDisputeMetrics(
            @RequestHeader("profile_id") String profileId,
            @RequestBody DisputeMetricsRequest request) {
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
    public Mono<ResponseEntity<ApiEventMetricsResponse>> getApiEventMetrics(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody ApiEventMetricsRequest request) {
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
    public Mono<ResponseEntity<ApiEventMetricsResponse>> getMerchantApiEventMetrics(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody ApiEventMetricsRequest request) {
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
    public Mono<ResponseEntity<ApiEventMetricsResponse>> getOrgApiEventMetrics(
            @RequestHeader("org_id") String orgId,
            @RequestBody ApiEventMetricsRequest request) {
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
    public Mono<ResponseEntity<ApiEventMetricsResponse>> getProfileApiEventMetrics(
            @RequestHeader("profile_id") String profileId,
            @RequestBody ApiEventMetricsRequest request) {
        return analyticsService.getProfileApiEventMetrics(profileId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
}

