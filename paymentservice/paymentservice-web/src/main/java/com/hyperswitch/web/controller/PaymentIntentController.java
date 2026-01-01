package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.*;
import com.hyperswitch.core.payments.ConfirmPaymentRequest;
import com.hyperswitch.core.payments.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 * REST controller for payment intent operations (v2 API)
 */
@RestController
@RequestMapping("/api/v2/payments")
@Tag(name = "Payment Intents (v2)", description = "Payment intent operations using v2 API")
public class PaymentIntentController {

    private PaymentService paymentService;

    // Default constructor to allow bean creation even if dependencies are missing
    public PaymentIntentController() {
        // PaymentService will be injected via setter if available
    }

    @Autowired(required = false)
    public void setPaymentService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }


    /**
     * Create payment intent (v2 API)
     * POST /api/v2/payments/create-intent
     */
    @PostMapping("/create-intent")
    @Operation(
        summary = "Create a payment intent (v2)",
        description = "Creates a payment intent without immediately confirming it. The payment will be in 'requires_confirmation' status."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment intent created successfully",
            content = @Content(schema = @Schema(implementation = PaymentsIntentResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid payment intent request"
        )
    })
    public Mono<ResponseEntity<PaymentsIntentResponse>> createPaymentIntent(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @Parameter(description = "Payment intent creation request", required = true)
            @RequestBody PaymentsCreateIntentRequest request) {
        if (paymentService == null) {
            return Mono.just(ResponseEntity.status(503)
                .header("X-Error", "PaymentService not available")
                .body(new PaymentsIntentResponse()));
        }
        return paymentService.createPaymentIntentV2(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Get payment intent (v2 API)
     * GET /api/v2/payments/{payment_id}/get-intent
     */
    @GetMapping("/{payment_id}/get-intent")
    @Operation(
        summary = "Get a payment intent (v2)",
        description = "Retrieves a payment intent by its ID using v2 API."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment intent retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentsIntentResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Payment intent not found"
        )
    })
    public Mono<ResponseEntity<PaymentsIntentResponse>> getPaymentIntent(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @Parameter(description = "Payment ID", required = true)
            @PathVariable("payment_id") String paymentId) {
        if (paymentService == null) {
            return Mono.just(ResponseEntity.status(503)
                .header("X-Error", "PaymentService not available")
                .body(new PaymentsIntentResponse()));
        }
        return paymentService.getPaymentIntentV2(paymentId, merchantId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Update payment intent (v2 API)
     * PUT /api/v2/payments/{payment_id}/update-intent
     */
    @PutMapping("/{payment_id}/update-intent")
    @Operation(
        summary = "Update a payment intent (v2)",
        description = "Updates a payment intent. Only fields provided in the request will be updated."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment intent updated successfully",
            content = @Content(schema = @Schema(implementation = PaymentsIntentResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Payment intent not found"
        )
    })
    public Mono<ResponseEntity<PaymentsIntentResponse>> updatePaymentIntent(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @Parameter(description = "Payment ID", required = true)
            @PathVariable("payment_id") String paymentId,
            @Parameter(description = "Payment intent update request", required = true)
            @RequestBody PaymentsUpdateIntentRequest request) {
        if (paymentService == null) {
            return Mono.just(ResponseEntity.status(503)
                .header("X-Error", "PaymentService not available")
                .body(new PaymentsIntentResponse()));
        }
        return paymentService.updatePaymentIntentV2(paymentId, merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Confirm payment intent (v2 API)
     * POST /api/v2/payments/{payment_id}/confirm-intent
     */
    @PostMapping("/{payment_id}/confirm-intent")
    @Operation(
        summary = "Confirm a payment intent (v2)",
        description = "Confirms a payment intent and processes the payment. The payment will be authorized with the connector."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment intent confirmed successfully",
            content = @Content(schema = @Schema(implementation = PaymentsIntentResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid confirmation request"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Payment intent not found"
        )
    })
    public Mono<ResponseEntity<PaymentsIntentResponse>> confirmPaymentIntent(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @Parameter(description = "Payment ID", required = true)
            @PathVariable("payment_id") String paymentId,
            @Parameter(description = "Payment confirmation request", required = true)
            @RequestBody ConfirmPaymentRequest request) {
        if (paymentService == null) {
            return Mono.just(ResponseEntity.status(503)
                .header("X-Error", "PaymentService not available")
                .body(new PaymentsIntentResponse()));
        }
        return paymentService.confirmPaymentIntentV2(paymentId, merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Create and confirm payment intent (v2 API)
     * POST /api/v2/payments
     */
    @PostMapping
    @Operation(
        summary = "Create and confirm payment intent (v2)",
        description = "Creates a payment intent and immediately confirms it in a single operation."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment intent created and confirmed successfully",
            content = @Content(schema = @Schema(implementation = PaymentsIntentResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid payment request"
        )
    })
    public Mono<ResponseEntity<PaymentsIntentResponse>> createAndConfirmPaymentIntent(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @Parameter(description = "Payment creation request", required = true)
            @RequestBody CreatePaymentRequest request) {
        if (paymentService == null) {
            return Mono.just(ResponseEntity.status(503)
                .header("X-Error", "PaymentService not available")
                .body(new PaymentsIntentResponse()));
        }
        return paymentService.createAndConfirmPaymentIntentV2(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Start payment redirection (v2 API)
     * GET /api/v2/payments/{payment_id}/start-redirection
     */
    @GetMapping("/{payment_id}/start-redirection")
    @Operation(
        summary = "Start payment redirection (v2)",
        description = "Starts the payment redirection flow and returns a redirect URL."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment redirection started successfully",
            content = @Content(schema = @Schema(implementation = PaymentsStartRedirectionResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Payment not found"
        )
    })
    public Mono<ResponseEntity<PaymentsStartRedirectionResponse>> startPaymentRedirection(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @Parameter(description = "Payment ID", required = true)
            @PathVariable("payment_id") String paymentId) {
        if (paymentService == null) {
            return Mono.just(ResponseEntity.status(503)
                .header("X-Error", "PaymentService not available")
                .body(new PaymentsStartRedirectionResponse()));
        }
        return paymentService.startPaymentRedirectionV2(paymentId, merchantId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Finish payment redirection (v2 API)
     * GET /api/v2/payments/{payment_id}/finish-redirection/{publishable_key}/{profile_id}
     */
    @GetMapping("/{payment_id}/finish-redirection/{publishable_key}/{profile_id}")
    @Operation(
        summary = "Finish payment redirection (v2)",
        description = "Completes the payment redirection flow after the user returns from the connector."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment redirection finished successfully",
            content = @Content(schema = @Schema(implementation = PaymentsFinishRedirectionResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Payment not found"
        )
    })
    public Mono<ResponseEntity<PaymentsFinishRedirectionResponse>> finishPaymentRedirection(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable("payment_id") String paymentId,
            @Parameter(description = "Publishable key", required = true)
            @PathVariable("publishable_key") String publishableKey,
            @Parameter(description = "Profile ID", required = true)
            @PathVariable("profile_id") String profileId) {
        if (paymentService == null) {
            return Mono.just(ResponseEntity.status(503)
                .header("X-Error", "PaymentService not available")
                .body(new PaymentsFinishRedirectionResponse()));
        }
        return paymentService.finishPaymentRedirectionV2(paymentId, publishableKey, profileId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Create external SDK tokens (v2 API)
     * POST /api/v2/payments/{payment_id}/create-external-sdk-tokens
     */
    @PostMapping("/{payment_id}/create-external-sdk-tokens")
    @Operation(
        summary = "Create external SDK tokens (v2)",
        description = "Creates session tokens for external SDKs (e.g., Stripe, PayPal) to enable client-side payment processing."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "External SDK tokens created successfully",
            content = @Content(schema = @Schema(implementation = PaymentsSessionResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Payment not found"
        )
    })
    public Mono<ResponseEntity<PaymentsSessionResponse>> createExternalSdkTokens(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @Parameter(description = "Payment ID", required = true)
            @PathVariable("payment_id") String paymentId,
            @Parameter(description = "Session request", required = true)
            @RequestBody PaymentsSessionRequest request) {
        if (paymentService == null) {
            return Mono.just(ResponseEntity.status(503)
                .header("X-Error", "PaymentService not available")
                .body(new PaymentsSessionResponse()));
        }
        return paymentService.createExternalSdkTokensV2(paymentId, merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Post session tokens (v1 API)
     * POST /api/payments/{payment_id}/post_session_tokens
     */
    @PostMapping("/{payment_id}/post_session_tokens")
    @Operation(
        summary = "Post session tokens (v1)",
        description = "Posts session tokens to a payment for connector-specific session management."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Session tokens posted successfully",
            content = @Content(schema = @Schema(implementation = PaymentsPostSessionTokensResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Payment not found"
        )
    })
    public Mono<ResponseEntity<PaymentsPostSessionTokensResponse>> postSessionTokens(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @Parameter(description = "Payment ID", required = true)
            @PathVariable("payment_id") String paymentId,
            @Parameter(description = "Session tokens request", required = true)
            @RequestBody PaymentsPostSessionTokensRequest request) {
        if (paymentService == null) {
            return Mono.just(ResponseEntity.status(503)
                .header("X-Error", "PaymentService not available")
                .body(new PaymentsPostSessionTokensResponse()));
        }
        return paymentService.postSessionTokens(paymentId, merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Create session tokens (v1 API)
     * POST /api/payments/session_tokens
     */
    @PostMapping("/session_tokens")
    @Operation(
        summary = "Create session tokens (v1)",
        description = "Creates session tokens for a payment session."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Session tokens created successfully",
            content = @Content(schema = @Schema(implementation = PaymentsSessionResponse.class))
        )
    })
    public Mono<ResponseEntity<PaymentsSessionResponse>> createSessionTokens(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @Parameter(description = "Session request", required = true)
            @RequestBody PaymentsSessionRequest request) {
        if (paymentService == null) {
            return Mono.just(ResponseEntity.status(503)
                .header("X-Error", "PaymentService not available")
                .body(new PaymentsSessionResponse()));
        }
        return paymentService.createSessionTokens(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Proxy confirm intent (v2 API)
     * POST /api/v2/payments/{payment_id}/proxy-confirm-intent
     */
    @PostMapping("/{payment_id}/proxy-confirm-intent")
    @Operation(
        summary = "Proxy confirm payment intent (v2)",
        description = "Proxies the confirmation of a payment intent through an external service"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment intent confirmed successfully",
            content = @Content(schema = @Schema(implementation = PaymentsIntentResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Payment intent not found")
    })
    public Mono<ResponseEntity<PaymentsIntentResponse>> proxyConfirmIntent(
            @PathVariable("payment_id") String paymentId,
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestBody(required = false) java.util.Map<String, Object> request) {
        if (paymentService == null) {
            return Mono.just(ResponseEntity.status(503)
                .header("X-Error", "PaymentService not available")
                .body(new PaymentsIntentResponse()));
        }
        return paymentService.proxyConfirmIntent(paymentId, merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Confirm intent with external vault proxy (v2 API)
     * POST /api/v2/payments/{payment_id}/confirm-intent/external-vault-proxy
     */
    @PostMapping("/{payment_id}/confirm-intent/external-vault-proxy")
    @Operation(
        summary = "Confirm intent with external vault proxy (v2)",
        description = "Confirms a payment intent using an external vault proxy"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment intent confirmed successfully",
            content = @Content(schema = @Schema(implementation = PaymentsIntentResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Payment intent not found")
    })
    public Mono<ResponseEntity<PaymentsIntentResponse>> confirmIntentWithExternalVaultProxy(
            @PathVariable("payment_id") String paymentId,
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestBody(required = false) java.util.Map<String, Object> request) {
        if (paymentService == null) {
            return Mono.just(ResponseEntity.status(503)
                .header("X-Error", "PaymentService not available")
                .body(new PaymentsIntentResponse()));
        }
        return paymentService.confirmIntentWithExternalVaultProxy(paymentId, merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get revenue recovery intent (v2 API)
     * GET /api/v2/payments/{payment_id}/get-revenue-recovery-intent
     */
    @GetMapping("/{payment_id}/get-revenue-recovery-intent")
    @Operation(
        summary = "Get revenue recovery intent (v2)",
        description = "Retrieves revenue recovery intent for a payment"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Revenue recovery intent retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentsIntentResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Payment intent not found")
    })
    public Mono<ResponseEntity<PaymentsIntentResponse>> getRevenueRecoveryIntent(
            @PathVariable("payment_id") String paymentId,
            @RequestHeader("X-Merchant-Id") String merchantId) {
        if (paymentService == null) {
            return Mono.just(ResponseEntity.status(503)
                .header("X-Error", "PaymentService not available")
                .body(new PaymentsIntentResponse()));
        }
        return paymentService.getRevenueRecoveryIntent(paymentId, merchantId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get payment status (v2 API)
     * GET /api/v2/payments/{payment_id}
     */
    @GetMapping("/{payment_id}")
    @Operation(
        summary = "Get payment status (v2)",
        description = "Retrieves payment status with gateway credentials (v2)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment status retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentsIntentResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Payment intent not found")
    })
    public Mono<ResponseEntity<PaymentsIntentResponse>> getPaymentStatusV2(
            @PathVariable("payment_id") String paymentId,
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestParam(required = false) Boolean forceSync) {
        if (paymentService == null) {
            return Mono.just(ResponseEntity.status(503)
                .header("X-Error", "PaymentService not available")
                .body(new PaymentsIntentResponse()));
        }
        return paymentService.getPaymentStatusV2(paymentId, merchantId, forceSync)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get payment intent by merchant reference ID (v2 API)
     * GET /api/v2/payments/ref/{merchant_reference_id}
     */
    @GetMapping("/ref/{merchant_reference_id}")
    @Operation(
        summary = "Get payment intent by merchant reference ID (v2)",
        description = "Retrieves a payment intent using the merchant's reference ID (v2)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment intent retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentsIntentResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Payment intent not found")
    })
    public Mono<ResponseEntity<PaymentsIntentResponse>> getPaymentIntentByMerchantReferenceIdV2(
            @PathVariable("merchant_reference_id") String merchantReferenceId,
            @RequestHeader("X-Merchant-Id") String merchantId) {
        if (paymentService == null) {
            return Mono.just(ResponseEntity.status(503)
                .header("X-Error", "PaymentService not available")
                .body(new PaymentsIntentResponse()));
        }
        return paymentService.getPaymentIntentByMerchantReferenceIdV2(merchantId, merchantReferenceId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Create recovery payment (v2 API)
     * POST /api/v2/payments/recovery
     */
    @PostMapping("/recovery")
    @Operation(
        summary = "Create recovery payment (v2)",
        description = "Creates a recovery payment for a failed payment"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Recovery payment created successfully",
            content = @Content(schema = @Schema(implementation = PaymentsIntentResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<PaymentsIntentResponse>> createRecoveryPayment(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestBody com.hyperswitch.common.dto.CreatePaymentRequest request) {
        if (paymentService == null) {
            return Mono.just(ResponseEntity.status(503)
                .header("X-Error", "PaymentService not available")
                .body(new PaymentsIntentResponse()));
        }
        return paymentService.createRecoveryPayment(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
}

