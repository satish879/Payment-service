package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.PaymentFiltersRequest;
import com.hyperswitch.common.dto.PaymentFiltersResponse;
import com.hyperswitch.common.dto.PaymentIntentFiltersRequest;
import com.hyperswitch.common.dto.PaymentIntentFiltersResponse;
import com.hyperswitch.common.dto.RefundFiltersRequest;
import com.hyperswitch.common.dto.RefundFiltersResponse;
import com.hyperswitch.common.dto.RoutingFiltersRequest;
import com.hyperswitch.common.dto.RoutingFiltersResponse;
import com.hyperswitch.common.dto.AuthEventFiltersRequest;
import com.hyperswitch.common.dto.AuthEventFiltersResponse;
import com.hyperswitch.common.dto.SdkEventFiltersRequest;
import com.hyperswitch.common.dto.SdkEventFiltersResponse;
import com.hyperswitch.common.dto.FrmFiltersRequest;
import com.hyperswitch.common.dto.FrmFiltersResponse;
import com.hyperswitch.common.dto.DisputeFiltersRequest;
import com.hyperswitch.common.dto.DisputeFiltersResponse;
import com.hyperswitch.common.dto.ApiEventFiltersRequest;
import com.hyperswitch.common.dto.ApiEventFiltersResponse;
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
 * REST controller for analytics filters operations
 */
@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics Filters", description = "Analytics filters operations")
public class AnalyticsFiltersController {
    
    private final AnalyticsService analyticsService;
    
    @Autowired
    public AnalyticsFiltersController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }
    
    /**
     * Get payment filters
     * POST /api/analytics/v1/filters/payments
     */
    @PostMapping("/v1/filters/payments")
    @Operation(
        summary = "Get payment filters",
        description = "Retrieves available filter values for payments"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<PaymentFiltersResponse>> getPaymentFilters(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody PaymentFiltersRequest request) {
        return analyticsService.getPaymentFilters(merchantId != null ? merchantId : "default", request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get merchant payment filters
     * POST /api/analytics/v1/merchant/filters/payments
     */
    @PostMapping("/v1/merchant/filters/payments")
    @Operation(
        summary = "Get merchant payment filters",
        description = "Retrieves available filter values for payments for a specific merchant"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Merchant payment filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<PaymentFiltersResponse>> getMerchantPaymentFilters(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody PaymentFiltersRequest request) {
        return analyticsService.getMerchantPaymentFilters(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get org payment filters
     * POST /api/analytics/v1/org/filters/payments
     */
    @PostMapping("/v1/org/filters/payments")
    @Operation(
        summary = "Get org payment filters",
        description = "Retrieves available filter values for payments for an organization"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Org payment filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<PaymentFiltersResponse>> getOrgPaymentFilters(
            @RequestHeader("org_id") String orgId,
            @RequestBody PaymentFiltersRequest request) {
        return analyticsService.getOrgPaymentFilters(orgId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get profile payment filters
     * POST /api/analytics/v1/profile/filters/payments
     */
    @PostMapping("/v1/profile/filters/payments")
    @Operation(
        summary = "Get profile payment filters",
        description = "Retrieves available filter values for payments for a specific profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile payment filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<PaymentFiltersResponse>> getProfilePaymentFilters(
            @RequestHeader("profile_id") String profileId,
            @RequestBody PaymentFiltersRequest request) {
        return analyticsService.getProfilePaymentFilters(profileId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get payment filters (v2)
     * POST /api/analytics/v2/filters/payments
     */
    @PostMapping("/v2/filters/payments")
    @Operation(
        summary = "Get payment filters (v2)",
        description = "Retrieves available filter values for payments using v2 API"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<PaymentFiltersResponse>> getPaymentFiltersV2(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody PaymentFiltersRequest request) {
        return analyticsService.getPaymentFilters(merchantId != null ? merchantId : "default", request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get merchant payment filters (v2)
     * POST /api/analytics/v2/merchant/filters/payments
     */
    @PostMapping("/v2/merchant/filters/payments")
    @Operation(
        summary = "Get merchant payment filters (v2)",
        description = "Retrieves available filter values for payments for a specific merchant using v2 API"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Merchant payment filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<PaymentFiltersResponse>> getMerchantPaymentFiltersV2(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody PaymentFiltersRequest request) {
        return analyticsService.getMerchantPaymentFilters(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get org payment filters (v2)
     * POST /api/analytics/v2/org/filters/payments
     */
    @PostMapping("/v2/org/filters/payments")
    @Operation(
        summary = "Get org payment filters (v2)",
        description = "Retrieves available filter values for payments for an organization using v2 API"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Org payment filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<PaymentFiltersResponse>> getOrgPaymentFiltersV2(
            @RequestHeader("org_id") String orgId,
            @RequestBody PaymentFiltersRequest request) {
        return analyticsService.getOrgPaymentFilters(orgId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get profile payment filters (v2)
     * POST /api/analytics/v2/profile/filters/payments
     */
    @PostMapping("/v2/profile/filters/payments")
    @Operation(
        summary = "Get profile payment filters (v2)",
        description = "Retrieves available filter values for payments for a specific profile using v2 API"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile payment filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<PaymentFiltersResponse>> getProfilePaymentFiltersV2(
            @RequestHeader("profile_id") String profileId,
            @RequestBody PaymentFiltersRequest request) {
        return analyticsService.getProfilePaymentFilters(profileId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get payment intent filters
     * POST /api/analytics/v1/filters/payment_intents
     */
    @PostMapping("/v1/filters/payment_intents")
    @Operation(
        summary = "Get payment intent filters",
        description = "Retrieves available filter values for payment intents"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment intent filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentIntentFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<PaymentIntentFiltersResponse>> getPaymentIntentFilters(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody PaymentIntentFiltersRequest request) {
        return analyticsService.getPaymentIntentFilters(merchantId != null ? merchantId : "default", request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get refund filters
     * POST /api/analytics/v1/filters/refunds
     */
    @PostMapping("/v1/filters/refunds")
    @Operation(
        summary = "Get refund filters",
        description = "Retrieves available filter values for refunds"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Refund filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = RefundFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<RefundFiltersResponse>> getRefundFilters(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody RefundFiltersRequest request) {
        return analyticsService.getRefundFilters(merchantId != null ? merchantId : "default", request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get merchant refund filters
     * POST /api/analytics/v1/merchant/filters/refunds
     */
    @PostMapping("/v1/merchant/filters/refunds")
    @Operation(
        summary = "Get merchant refund filters",
        description = "Retrieves available filter values for refunds for a specific merchant"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Merchant refund filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = RefundFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<RefundFiltersResponse>> getMerchantRefundFilters(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody RefundFiltersRequest request) {
        return analyticsService.getMerchantRefundFilters(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get org refund filters
     * POST /api/analytics/v1/org/filters/refunds
     */
    @PostMapping("/v1/org/filters/refunds")
    @Operation(
        summary = "Get org refund filters",
        description = "Retrieves available filter values for refunds for an organization"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Org refund filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = RefundFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<RefundFiltersResponse>> getOrgRefundFilters(
            @RequestHeader("org_id") String orgId,
            @RequestBody RefundFiltersRequest request) {
        return analyticsService.getOrgRefundFilters(orgId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get profile refund filters
     * POST /api/analytics/v1/profile/filters/refunds
     */
    @PostMapping("/v1/profile/filters/refunds")
    @Operation(
        summary = "Get profile refund filters",
        description = "Retrieves available filter values for refunds for a specific profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile refund filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = RefundFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<RefundFiltersResponse>> getProfileRefundFilters(
            @RequestHeader("profile_id") String profileId,
            @RequestBody RefundFiltersRequest request) {
        return analyticsService.getProfileRefundFilters(profileId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get routing filters
     * POST /api/analytics/v1/filters/routing
     */
    @PostMapping("/v1/filters/routing")
    @Operation(
        summary = "Get routing filters",
        description = "Retrieves available filter values for routing"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Routing filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = RoutingFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<RoutingFiltersResponse>> getRoutingFilters(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody RoutingFiltersRequest request) {
        return analyticsService.getRoutingFilters(merchantId != null ? merchantId : "default", request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get merchant routing filters
     * POST /api/analytics/v1/merchant/filters/routing
     */
    @PostMapping("/v1/merchant/filters/routing")
    @Operation(
        summary = "Get merchant routing filters",
        description = "Retrieves available filter values for routing for a specific merchant"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Merchant routing filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = RoutingFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<RoutingFiltersResponse>> getMerchantRoutingFilters(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody RoutingFiltersRequest request) {
        return analyticsService.getMerchantRoutingFilters(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get org routing filters
     * POST /api/analytics/v1/org/filters/routing
     */
    @PostMapping("/v1/org/filters/routing")
    @Operation(
        summary = "Get org routing filters",
        description = "Retrieves available filter values for routing for an organization"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Org routing filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = RoutingFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<RoutingFiltersResponse>> getOrgRoutingFilters(
            @RequestHeader("org_id") String orgId,
            @RequestBody RoutingFiltersRequest request) {
        return analyticsService.getOrgRoutingFilters(orgId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get profile routing filters
     * POST /api/analytics/v1/profile/filters/routing
     */
    @PostMapping("/v1/profile/filters/routing")
    @Operation(
        summary = "Get profile routing filters",
        description = "Retrieves available filter values for routing for a specific profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile routing filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = RoutingFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<RoutingFiltersResponse>> getProfileRoutingFilters(
            @RequestHeader("profile_id") String profileId,
            @RequestBody RoutingFiltersRequest request) {
        return analyticsService.getProfileRoutingFilters(profileId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get auth event filters
     * POST /api/analytics/v1/filters/auth_events
     */
    @PostMapping("/v1/filters/auth_events")
    @Operation(
        summary = "Get auth event filters",
        description = "Retrieves available filter values for auth events"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Auth event filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = AuthEventFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<AuthEventFiltersResponse>> getAuthEventFilters(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody AuthEventFiltersRequest request) {
        return analyticsService.getAuthEventFilters(merchantId != null ? merchantId : "default", request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get merchant auth event filters
     * POST /api/analytics/v1/merchant/filters/auth_events
     */
    @PostMapping("/v1/merchant/filters/auth_events")
    @Operation(
        summary = "Get merchant auth event filters",
        description = "Retrieves available filter values for auth events for a specific merchant"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Merchant auth event filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = AuthEventFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<AuthEventFiltersResponse>> getMerchantAuthEventFilters(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody AuthEventFiltersRequest request) {
        return analyticsService.getMerchantAuthEventFilters(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get org auth event filters
     * POST /api/analytics/v1/org/filters/auth_events
     */
    @PostMapping("/v1/org/filters/auth_events")
    @Operation(
        summary = "Get org auth event filters",
        description = "Retrieves available filter values for auth events for an organization"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Org auth event filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = AuthEventFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<AuthEventFiltersResponse>> getOrgAuthEventFilters(
            @RequestHeader("org_id") String orgId,
            @RequestBody AuthEventFiltersRequest request) {
        return analyticsService.getOrgAuthEventFilters(orgId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get profile auth event filters
     * POST /api/analytics/v1/profile/filters/auth_events
     */
    @PostMapping("/v1/profile/filters/auth_events")
    @Operation(
        summary = "Get profile auth event filters",
        description = "Retrieves available filter values for auth events for a specific profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile auth event filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = AuthEventFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<AuthEventFiltersResponse>> getProfileAuthEventFilters(
            @RequestHeader("profile_id") String profileId,
            @RequestBody AuthEventFiltersRequest request) {
        return analyticsService.getProfileAuthEventFilters(profileId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get SDK event filters
     * POST /api/analytics/v1/filters/sdk_events
     */
    @PostMapping("/v1/filters/sdk_events")
    @Operation(
        summary = "Get SDK event filters",
        description = "Retrieves available filter values for SDK events"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "SDK event filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = SdkEventFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<SdkEventFiltersResponse>> getSdkEventFilters(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody SdkEventFiltersRequest request) {
        return analyticsService.getSdkEventFilters(merchantId != null ? merchantId : "default", request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get FRM filters
     * POST /api/analytics/v1/filters/frm
     */
    @PostMapping("/v1/filters/frm")
    @Operation(
        summary = "Get FRM filters",
        description = "Retrieves available filter values for FRM (fraud risk management)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "FRM filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = FrmFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<FrmFiltersResponse>> getFrmFilters(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody FrmFiltersRequest request) {
        return analyticsService.getFrmFilters(merchantId != null ? merchantId : "default", request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get dispute filters
     * POST /api/analytics/v1/filters/disputes
     */
    @PostMapping("/v1/filters/disputes")
    @Operation(
        summary = "Get dispute filters",
        description = "Retrieves available filter values for disputes"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Dispute filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = DisputeFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<DisputeFiltersResponse>> getDisputeFilters(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody DisputeFiltersRequest request) {
        return analyticsService.getDisputeFilters(merchantId != null ? merchantId : "default", request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get merchant dispute filters
     * POST /api/analytics/v1/merchant/filters/disputes
     */
    @PostMapping("/v1/merchant/filters/disputes")
    @Operation(
        summary = "Get merchant dispute filters",
        description = "Retrieves available filter values for disputes for a specific merchant"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Merchant dispute filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = DisputeFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<DisputeFiltersResponse>> getMerchantDisputeFilters(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody DisputeFiltersRequest request) {
        return analyticsService.getMerchantDisputeFilters(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get org dispute filters
     * POST /api/analytics/v1/org/filters/disputes
     */
    @PostMapping("/v1/org/filters/disputes")
    @Operation(
        summary = "Get org dispute filters",
        description = "Retrieves available filter values for disputes for an organization"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Org dispute filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = DisputeFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<DisputeFiltersResponse>> getOrgDisputeFilters(
            @RequestHeader("org_id") String orgId,
            @RequestBody DisputeFiltersRequest request) {
        return analyticsService.getOrgDisputeFilters(orgId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get profile dispute filters
     * POST /api/analytics/v1/profile/filters/disputes
     */
    @PostMapping("/v1/profile/filters/disputes")
    @Operation(
        summary = "Get profile dispute filters",
        description = "Retrieves available filter values for disputes for a specific profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile dispute filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = DisputeFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<DisputeFiltersResponse>> getProfileDisputeFilters(
            @RequestHeader("profile_id") String profileId,
            @RequestBody DisputeFiltersRequest request) {
        return analyticsService.getProfileDisputeFilters(profileId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get API event filters
     * POST /api/analytics/v1/filters/api_events
     */
    @PostMapping("/v1/filters/api_events")
    @Operation(
        summary = "Get API event filters",
        description = "Retrieves available filter values for API events"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "API event filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = ApiEventFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<ApiEventFiltersResponse>> getApiEventFilters(
            @RequestHeader(value = "merchant_id", required = false) String merchantId,
            @RequestBody ApiEventFiltersRequest request) {
        return analyticsService.getApiEventFilters(merchantId != null ? merchantId : "default", request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get merchant API event filters
     * POST /api/analytics/v1/merchant/filters/api_events
     */
    @PostMapping("/v1/merchant/filters/api_events")
    @Operation(
        summary = "Get merchant API event filters",
        description = "Retrieves available filter values for API events for a specific merchant"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Merchant API event filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = ApiEventFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<ApiEventFiltersResponse>> getMerchantApiEventFilters(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody ApiEventFiltersRequest request) {
        return analyticsService.getMerchantApiEventFilters(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get org API event filters
     * POST /api/analytics/v1/org/filters/api_events
     */
    @PostMapping("/v1/org/filters/api_events")
    @Operation(
        summary = "Get org API event filters",
        description = "Retrieves available filter values for API events for an organization"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Org API event filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = ApiEventFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<ApiEventFiltersResponse>> getOrgApiEventFilters(
            @RequestHeader("org_id") String orgId,
            @RequestBody ApiEventFiltersRequest request) {
        return analyticsService.getOrgApiEventFilters(orgId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get profile API event filters
     * POST /api/analytics/v1/profile/filters/api_events
     */
    @PostMapping("/v1/profile/filters/api_events")
    @Operation(
        summary = "Get profile API event filters",
        description = "Retrieves available filter values for API events for a specific profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile API event filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = ApiEventFiltersResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<ApiEventFiltersResponse>> getProfileApiEventFilters(
            @RequestHeader("profile_id") String profileId,
            @RequestBody ApiEventFiltersRequest request) {
        return analyticsService.getProfileApiEventFilters(profileId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
}

