package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.CreatePaymentRequest;
import com.hyperswitch.common.dto.UpdateRefundRequest;
import com.hyperswitch.common.dto.RefundAggregatesResponse;
import com.hyperswitch.common.types.PaymentId;
import com.hyperswitch.core.payments.*;
import com.hyperswitch.web.controller.PaymentException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import jakarta.annotation.PostConstruct;
import java.util.Map;

/**
 * REST API controller for payment operations
 */
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Payment processing operations including creation, confirmation, capture, refunds, and 3DS authentication")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private PaymentService paymentService;
    private com.hyperswitch.core.paymentmethods.PaymentMethodService paymentMethodService;
    private com.hyperswitch.core.revenuerecovery.RevenueRecoveryService revenueRecoveryService;

    // Constructor injection for required dependencies
    @Autowired
    public PaymentController(PaymentService paymentService, 
                           com.hyperswitch.core.paymentmethods.PaymentMethodService paymentMethodService,
                           @Autowired(required = false) com.hyperswitch.core.revenuerecovery.RevenueRecoveryService revenueRecoveryService) {
        this.paymentService = paymentService;
        this.paymentMethodService = paymentMethodService;
        this.revenueRecoveryService = revenueRecoveryService;
        log.info("PaymentController created with PaymentService: {}, PaymentMethodService: {}, RevenueRecoveryService: {}", 
                paymentService != null ? "OK" : "NULL", 
                paymentMethodService != null ? "OK" : "NULL",
                revenueRecoveryService != null ? "OK" : "NULL");
    }

    
    @PostConstruct
    public void init() {
        log.info("=== PaymentController BEAN CREATED ===");
        log.info("PaymentService available: {}", paymentService != null);
        log.info("PaymentMethodService available: {}", paymentMethodService != null);
        log.info("RevenueRecoveryService available: {}", revenueRecoveryService != null);
        log.info("Payment endpoints registered: /api/payments");
        
        if (paymentService == null) {
            log.warn("PaymentService is not available - payment endpoints will not function properly");
        }
    }

    /**
     * Health check for payments endpoint
     * GET /api/payments
     */
    @GetMapping
    @Operation(summary = "Payments endpoint health check", description = "Returns status to verify payments endpoint is accessible")
    public Mono<ResponseEntity<Map<String, String>>> paymentsHealth() {
        return Mono.just(ResponseEntity.ok(Map.of("status", "ok", "endpoint", "/api/payments")));
    }

    @PostMapping
    @Operation(
        summary = "Create a payment", 
        description = "Creates a new payment intent with the specified amount, currency, and payment method. " +
                     "The payment will be in 'requires_confirmation' status until confirmed.",
        requestBody = @RequestBody(
            description = "Payment creation request",
            required = true,
            content = @Content(
                examples = {
                    @ExampleObject(
                        name = "Card Payment",
                        summary = "Example: Card payment",
                        value = """
                            {
                              "amount": {
                                "value": 1000,
                                "currencyCode": "USD"
                              },
                              "merchantId": "merchant_123",
                              "paymentMethod": "CARD",
                              "customerId": "cust_123",
                              "description": "Payment for order #12345",
                              "captureMethod": "AUTOMATIC",
                              "confirm": false
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Setup Mandate",
                        summary = "Example: Setup mandate for recurring payments",
                        value = """
                            {
                              "amount": {
                                "value": 0,
                                "currencyCode": "USD"
                              },
                              "merchantId": "merchant_123",
                              "paymentMethod": "CARD",
                              "customerId": "cust_123",
                              "paymentType": "setup_mandate",
                              "confirm": true
                            }
                            """
                    )
                }
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", 
            description = "Payment created successfully",
            content = @Content(
                schema = @Schema(implementation = PaymentIntent.class),
                examples = {
                    @ExampleObject(
                        name = "Success Response",
                        summary = "Payment created",
                        value = """
                            {
                              "paymentId": "pay_abc123",
                              "status": "requires_confirmation",
                              "amount": 1000,
                              "currency": "USD",
                              "clientSecret": "pi_abc123_secret_xyz",
                              "createdAt": "2024-01-15T10:30:00Z"
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid request parameters or payment error",
            content = @Content(
                schema = @Schema(implementation = com.hyperswitch.common.dto.ErrorResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Validation Error",
                        ref = "#/components/examples/errorValidation"
                    ),
                    @ExampleObject(
                        name = "Insufficient Funds",
                        ref = "#/components/examples/errorInsufficientFunds"
                    )
                }
            )
        ),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Mono<ResponseEntity<PaymentIntent>> createPayment(
            @RequestHeader(value = "X-Merchant-Id", required = false) String merchantIdHeader,
            @Parameter(
                description = "Payment creation request", 
                required = true,
                examples = {
                    @ExampleObject(
                        name = "Card Payment",
                        value = "{\"amount\": {\"value\": 1000, \"currencyCode\": \"USD\"}, \"merchantId\": \"merchant_123\", \"paymentMethod\": \"CARD\"}"
                    )
                }
            )
            @org.springframework.web.bind.annotation.RequestBody CreatePaymentRequest request, org.springframework.web.server.ServerWebExchange exchange) {
        log.info("=== PaymentController.createPayment() CALLED ===");
        log.info("Request object: {}", request);
        log.info("Request is null: {}", request == null);
        
        if (request == null) {
            log.error("CreatePaymentRequest is null! This indicates deserialization failed completely.");
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        // Set merchantId from header if not already set in request body (header takes precedence)
        if (merchantIdHeader != null && (request.getMerchantId() == null || request.getMerchantId().isEmpty())) {
            request.setMerchantId(merchantIdHeader);
        }
        
        log.info("Request merchantId: {}", request.getMerchantId());
        log.info("Request amount: {}", request.getAmount());
        log.info("Request amount is null: {}", request.getAmount() == null);
        
        if (request.getAmount() == null) {
            log.error("Amount field is null in CreatePaymentRequest. This suggests AmountDeserializer was not called or failed silently.");
            log.error("Full request object details: {}", request);
            // Fallback: check if RequestLoggingFilter stored a directly-parsed object on the exchange
            CreatePaymentRequest cached = exchange.getAttribute("directCreatePaymentRequest");
            if (cached != null) {
                log.warn("Found cached direct CreatePaymentRequest on exchange attributes - using it as fallback: merchantId={}, amount={}", cached.getMerchantId(), cached.getAmount());
                System.out.println("[STDOUT] PaymentController: Found cached direct CreatePaymentRequest - merchantId=" + cached.getMerchantId() + " amount=" + cached.getAmount());
                request = cached;
                // Re-apply merchantId from header if available
                if (merchantIdHeader != null && (request.getMerchantId() == null || request.getMerchantId().isEmpty())) {
                    request.setMerchantId(merchantIdHeader);
                }
            }
        }
        
        log.debug("Full request: {}", request);
        
        return paymentService.createPayment(request)
                .map(result -> {
                    if (result.isOk()) {
                        log.info("Payment created successfully: {}", result.unwrap().getPaymentId());
                        return ResponseEntity.status(HttpStatus.CREATED).body(result.unwrap());
                    } else {
                        log.warn("Payment creation failed: {}", result.unwrapErr().getMessage());
                        throw new PaymentException(result.unwrapErr());
                    }
                });
    }

    @PostMapping("/{paymentId}/confirm")
    @Operation(
        summary = "Confirm a payment", 
        description = "Confirms a payment intent and initiates the payment processing. " +
                     "For 3DS payments, this may return a challenge URL that needs to be completed.",
        requestBody = @RequestBody(
            description = "Payment confirmation request",
            required = true,
            content = @Content(
                examples = {
                    @ExampleObject(
                        name = "Card Payment",
                        summary = "Example: Confirm card payment",
                        value = """
                            {
                              "paymentMethodId": "pm_123",
                              "returnUrl": "https://merchant.com/return"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "MIT Payment",
                        summary = "Example: Merchant-initiated transaction",
                        value = """
                            {
                              "paymentMethodId": "pm_123",
                              "recurringDetails": {
                                "mandateId": "mandate_123"
                              },
                              "offSession": true
                            }
                            """
                    )
                }
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Payment confirmed successfully",
            content = @Content(
                schema = @Schema(implementation = PaymentIntent.class),
                examples = {
                    @ExampleObject(
                        name = "Success Response",
                        summary = "Payment confirmed",
                        value = """
                            {
                              "paymentId": "pay_abc123",
                              "status": "succeeded",
                              "amount": 1000,
                              "currency": "USD"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "3DS Challenge",
                        summary = "3DS authentication required",
                        value = """
                            {
                              "paymentId": "pay_abc123",
                              "status": "requires_authentication",
                              "nextAction": {
                                "type": "redirect",
                                "url": "https://3ds.example.com/challenge"
                              }
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid payment or request",
            content = @Content(
                schema = @Schema(implementation = com.hyperswitch.common.dto.ErrorResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Card Declined",
                        ref = "#/components/examples/errorCardDeclined"
                    ),
                    @ExampleObject(
                        name = "Insufficient Funds",
                        ref = "#/components/examples/errorInsufficientFunds"
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Payment not found",
            content = @Content(
                schema = @Schema(implementation = com.hyperswitch.common.dto.ErrorResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Not Found",
                        ref = "#/components/examples/errorNotFound"
                    )
                }
            )
        )
    })
    public Mono<ResponseEntity<PaymentIntent>> confirmPayment(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable("paymentId") String paymentId,
            @Parameter(description = "Payment confirmation request", required = true)
            @RequestBody ConfirmPaymentRequest request) {
        return paymentService.confirmPayment(PaymentId.of(paymentId), request)
                .map(result -> {
                    if (result.isOk()) {
                        return ResponseEntity.ok(result.unwrap());
                    } else {
                        throw new PaymentException(result.unwrapErr());
                    }
                });
    }

    @PostMapping("/{paymentId}/capture")
    @Operation(summary = "Capture a payment", description = "Captures an authorized payment. Supports full and partial capture")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment captured successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid capture request"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public Mono<ResponseEntity<PaymentIntent>> capturePayment(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable("paymentId") String paymentId,
            @Parameter(description = "Capture request with amount", required = true)
            @RequestBody CapturePaymentRequest request) {
        return paymentService.capturePayment(PaymentId.of(paymentId), request)
                .map(result -> {
                    if (result.isOk()) {
                        return ResponseEntity.ok(result.unwrap());
                    } else {
                        throw new PaymentException(result.unwrapErr());
                    }
                });
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment details", description = "Retrieves the details of a payment by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment found"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public Mono<ResponseEntity<PaymentIntent>> getPayment(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable("paymentId") String paymentId) {
        return paymentService.getPayment(PaymentId.of(paymentId))
                .map(result -> {
                    if (result.isOk()) {
                        return ResponseEntity.ok(result.unwrap());
                    } else {
                        throw new PaymentException(result.unwrapErr());
                    }
                });
    }

    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "Create a refund", description = "Creates a refund for a payment. Supports full and partial refunds")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Refund created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid refund request"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public Mono<ResponseEntity<Refund>> refundPayment(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable("paymentId") String paymentId,
            @Parameter(description = "Refund request with amount", required = true)
            @RequestBody RefundRequest request) {
        return paymentService.refundPayment(PaymentId.of(paymentId), request)
                .map(result -> {
                    if (result.isOk()) {
                        return ResponseEntity.ok(result.unwrap());
                    } else {
                        throw new PaymentException(result.unwrapErr());
                    }
                });
    }

    /**
     * Handle 3DS challenge
     * POST /api/payments/{paymentId}/3ds/challenge
     */
    @PostMapping("/{paymentId}/3ds/challenge")
    public Mono<ResponseEntity<com.hyperswitch.common.dto.ThreeDSResponse>> handle3DSChallenge(
            @PathVariable("paymentId") String paymentId,
            @RequestBody com.hyperswitch.common.dto.ThreeDSRequest request) {
        return paymentService.handle3DSChallenge(PaymentId.of(paymentId), request)
                .map(result -> {
                    if (result.isOk()) {
                        return ResponseEntity.ok(result.unwrap());
                    } else {
                        throw new PaymentException(result.unwrapErr());
                    }
                });
    }

    /**
     * Resume payment after 3DS authentication
     * POST /api/payments/{paymentId}/3ds/resume
     */
    @PostMapping("/{paymentId}/3ds/resume")
    public Mono<ResponseEntity<PaymentIntent>> resumePaymentAfter3DS(
            @PathVariable("paymentId") String paymentId,
            @RequestBody(required = false) String requestBody,
            org.springframework.web.server.ServerWebExchange exchange) {
        log.debug("Resuming payment after 3DS: paymentId={}, requestBody={}", paymentId, requestBody);
        // Fallback: if framework didn't bind the request body, try the raw body captured by RequestLoggingFilter
        if (requestBody == null || requestBody.trim().isEmpty()) {
            String raw = exchange.getAttribute("rawRequestBody");
            if (raw != null && !raw.trim().isEmpty()) {
                log.warn("RequestBody was empty; using rawRequestBody from exchange attributes");
                requestBody = raw;
            }
        }

        if (requestBody == null || requestBody.trim().isEmpty()) {
            log.warn("Request body is null or empty for 3DS resume");
            throw new PaymentException(com.hyperswitch.common.errors.PaymentError.of("INVALID_REQUEST", "Request body is required"));
        }
        
        // Use ObjectMapper to parse the request
        String authenticationId = null;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(requestBody);
            if (jsonNode.has("authenticationId")) {
                authenticationId = jsonNode.get("authenticationId").asText();
            }
        } catch (Exception e) {
            log.error("Error parsing JSON request body: {}", e.getMessage(), e);
            throw new PaymentException(com.hyperswitch.common.errors.PaymentError.of("INVALID_REQUEST", "Invalid JSON format"));
        }
        
        log.debug("Extracted authenticationId: {}", authenticationId);
        if (authenticationId == null || authenticationId.isEmpty()) {
            log.warn("authenticationId is null or empty");
            throw new PaymentException(com.hyperswitch.common.errors.PaymentError.of("INVALID_REQUEST", "authenticationId is required"));
        }
        return paymentService.resumePaymentAfter3DS(PaymentId.of(paymentId), authenticationId)
                .map(result -> {
                    if (result.isOk()) {
                        return ResponseEntity.ok(result.unwrap());
                    } else {
                        throw new PaymentException(result.unwrapErr());
                    }
                });
    }

    /**
     * 3DS callback endpoint
     * POST /api/payments/{paymentId}/3ds/callback
     */
    @PostMapping("/{paymentId}/3ds/callback")
    public Mono<ResponseEntity<PaymentIntent>> handle3DSCallback(
            @PathVariable("paymentId") String paymentId,
            @RequestParam(value = "authenticationId", required = false) String authenticationId,
            @RequestBody(required = false) java.util.Map<String, Object> callbackData) {
        // Extract authenticationId from callback data if not in query param
        String authId = authenticationId;
        if (authId == null && callbackData != null) {
            Object authIdObj = callbackData.get("authentication_id");
            if (authIdObj != null) {
                authId = authIdObj.toString();
            }
        }
        
        if (authId == null) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        return paymentService.resumePaymentAfter3DS(PaymentId.of(paymentId), authId)
                .map(result -> {
                    if (result.isOk()) {
                        return ResponseEntity.ok(result.unwrap());
                    } else {
                        throw new PaymentException(result.unwrapErr());
                    }
                });
    }

    /**
     * Cancel a payment
     * POST /api/payments/{paymentId}/cancel
     */
    @PostMapping("/{paymentId}/cancel")
    public Mono<ResponseEntity<PaymentIntent>> cancelPayment(
            @PathVariable("paymentId") String paymentId,
            @RequestBody(required = false) com.hyperswitch.common.dto.CancelPaymentRequest request) {
        if (request == null) {
            request = com.hyperswitch.common.dto.CancelPaymentRequest.builder().build();
        }
        return paymentService.cancelPayment(PaymentId.of(paymentId), request)
                .map(result -> {
                    if (result.isOk()) {
                        return ResponseEntity.ok(result.unwrap());
                    } else {
                        throw new PaymentException(result.unwrapErr());
                    }
                });
    }

    /**
     * Update a payment
     * POST /api/payments/{paymentId}
     */
    @PostMapping(value = "/{paymentId}", consumes = "application/json")
    public Mono<ResponseEntity<PaymentIntent>> updatePayment(
            @PathVariable("paymentId") String paymentId,
            @RequestBody com.hyperswitch.common.dto.UpdatePaymentRequest request) {
        return paymentService.updatePayment(PaymentId.of(paymentId), request)
                .map(result -> {
                    if (result.isOk()) {
                        return ResponseEntity.ok(result.unwrap());
                    } else {
                        throw new PaymentException(result.unwrapErr());
                    }
                });
    }

    /**
     * Get client secret for a payment
     * GET /api/payments/{paymentId}/client_secret
     */
    @GetMapping("/{paymentId}/client_secret")
    public Mono<ResponseEntity<java.util.Map<String, String>>> getClientSecret(
            @PathVariable("paymentId") String paymentId) {
        return paymentService.getClientSecret(PaymentId.of(paymentId))
                .map(result -> {
                    if (result.isOk()) {
                        java.util.Map<String, String> response = new java.util.HashMap<>();
                        response.put("client_secret", result.unwrap());
                        return ResponseEntity.ok(response);
                    } else {
                        throw new PaymentException(result.unwrapErr());
                    }
                });
    }

    /**
     * Incremental authorization - increase authorized amount
     * POST /api/payments/{paymentId}/incremental_authorization
     */
    @PostMapping("/{paymentId}/incremental_authorization")
    public Mono<ResponseEntity<PaymentIntent>> incrementalAuthorization(
            @PathVariable("paymentId") String paymentId,
            @RequestBody com.hyperswitch.common.dto.IncrementalAuthorizationRequest request) {
        return paymentService.incrementalAuthorization(PaymentId.of(paymentId), request)
                .map(result -> {
                    if (result.isOk()) {
                        return ResponseEntity.ok(result.unwrap());
                    } else {
                        throw new PaymentException(result.unwrapErr());
                    }
                });
    }

    /**
     * Extend authorization - extend authorization validity period
     * POST /api/payments/{paymentId}/extend_authorization
     */
    @PostMapping("/{paymentId}/extend_authorization")
    public Mono<ResponseEntity<PaymentIntent>> extendAuthorization(
            @PathVariable("paymentId") String paymentId) {
        return paymentService.extendAuthorization(PaymentId.of(paymentId))
                .map(result -> {
                    if (result.isOk()) {
                        return ResponseEntity.ok(result.unwrap());
                    } else {
                        throw new PaymentException(result.unwrapErr());
                    }
                });
    }

    /**
     * Void payment - cancel an authorized payment before capture
     * POST /api/payments/{paymentId}/void
     */
    @PostMapping("/{paymentId}/void")
    public Mono<ResponseEntity<PaymentIntent>> voidPayment(
            @PathVariable("paymentId") String paymentId,
            @RequestBody(required = false) com.hyperswitch.common.dto.VoidPaymentRequest request) {
        if (request == null) {
            request = new com.hyperswitch.common.dto.VoidPaymentRequest();
        }
        return paymentService.voidPayment(PaymentId.of(paymentId), request)
                .map(result -> {
                    if (result.isOk()) {
                        return ResponseEntity.ok(result.unwrap());
                    } else {
                        throw new PaymentException(result.unwrapErr());
                    }
                });
    }

    /**
     * Schedule capture - schedule automatic capture of an authorized payment
     * POST /api/payments/{paymentId}/schedule_capture
     */
    @PostMapping("/{paymentId}/schedule_capture")
    public Mono<ResponseEntity<PaymentIntent>> scheduleCapture(
            @PathVariable("paymentId") String paymentId,
            @RequestBody com.hyperswitch.common.dto.ScheduleCaptureRequest request) {
        return paymentService.scheduleCapture(PaymentId.of(paymentId), request)
                .map(result -> {
                    if (result.isOk()) {
                        return ResponseEntity.ok(result.unwrap());
                    } else {
                        throw new PaymentException(result.unwrapErr());
                    }
                });
    }

    /**
     * Approve a payment
     * POST /api/payments/{paymentId}/approve
     */
    @PostMapping("/{paymentId}/approve")
    public Mono<ResponseEntity<PaymentIntent>> approvePayment(
            @PathVariable String paymentId,
            @RequestBody(required = false) com.hyperswitch.common.dto.ApprovePaymentRequest request) {
        if (request == null) {
            request = new com.hyperswitch.common.dto.ApprovePaymentRequest();
        }
        return paymentService.approvePayment(PaymentId.of(paymentId), request)
                .map(result -> {
                    if (result.isOk()) {
                        return ResponseEntity.ok(result.unwrap());
                    } else {
                        throw new PaymentException(result.unwrapErr());
                    }
                });
    }

    /**
     * Reject a payment
     * POST /api/payments/{paymentId}/reject
     */
    @PostMapping("/{paymentId}/reject")
    public Mono<ResponseEntity<PaymentIntent>> rejectPayment(
            @PathVariable String paymentId,
            @RequestBody com.hyperswitch.common.dto.RejectPaymentRequest request) {
        return paymentService.rejectPayment(PaymentId.of(paymentId), request)
                .map(result -> {
                    if (result.isOk()) {
                        return ResponseEntity.ok(result.unwrap());
                    } else {
                        throw new PaymentException(result.unwrapErr());
                    }
                });
    }

    /**
     * Sync payment status with connector (psync)
     * POST /api/payments/{paymentId}/sync
     */
    @PostMapping("/{paymentId}/sync")
    @Operation(summary = "Sync payment status", description = "Synchronizes payment status with the connector (psync)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment synced successfully"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public Mono<ResponseEntity<PaymentIntent>> syncPayment(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable String paymentId,
            @RequestBody(required = false) com.hyperswitch.common.dto.SyncPaymentRequest request) {
        if (request == null) {
            request = new com.hyperswitch.common.dto.SyncPaymentRequest();
        }
        return paymentService.syncPayment(PaymentId.of(paymentId), request)
                .map(result -> {
                    if (result.isOk()) {
                        return ResponseEntity.ok(result.unwrap());
                    } else {
                        throw new PaymentException(result.unwrapErr());
                    }
                });
    }
    
    /**
     * List payment attempts for a payment
     * GET /api/payments/{paymentId}/list-attempts
     */
    @GetMapping("/{paymentId}/list-attempts")
    @Operation(
        summary = "List payment attempts",
        description = "Retrieves all payment attempts for a specific payment. This is useful for tracking the history of payment processing attempts."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment attempts retrieved successfully",
            content = @Content(schema = @Schema(implementation = com.hyperswitch.common.dto.PaymentAttemptResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public Mono<ResponseEntity<reactor.core.publisher.Flux<com.hyperswitch.common.dto.PaymentAttemptResponse>>> listPaymentAttempts(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable String paymentId,
            @RequestHeader("X-Merchant-Id") String merchantId) {
        return paymentService.listPaymentAttempts(PaymentId.of(paymentId), merchantId)
                .map(result -> {
                    if (result.isOk()) {
                        return ResponseEntity.ok(result.unwrap());
                    } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * List payments with filters
     * GET /api/payments/list
     * POST /api/payments/list
     */
    @GetMapping("/list")
    @PostMapping("/list")
    @Operation(
        summary = "List payments with filters",
        description = "Retrieves a list of payments for a merchant with optional filtering by status, currency, connector, time range, amount, etc."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payments retrieved successfully",
            content = @Content(schema = @Schema(implementation = com.hyperswitch.common.dto.PaymentListResponse.class))
        )
    })
    public Mono<ResponseEntity<com.hyperswitch.common.dto.PaymentListResponse>> listPayments(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestBody(required = false) com.hyperswitch.common.dto.PaymentListFilterConstraints constraints) {
        if (constraints == null) {
            constraints = new com.hyperswitch.common.dto.PaymentListFilterConstraints();
        }
        return paymentService.listPayments(merchantId, constraints)
                .map(result -> {
                    if (result.isOk()) {
                        return ResponseEntity.ok(result.unwrap());
                    } else {
                        throw new PaymentException(result.unwrapErr());
                    }
                });
    }
    
    /**
     * Get available payment filters
     * GET /api/payments/filter
     */
    @GetMapping("/filter")
    @Operation(
        summary = "Get payment filters",
        description = "Retrieves available filter options for payment listing (connectors, currencies, statuses, payment methods, etc.)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = com.hyperswitch.common.dto.PaymentListFiltersResponse.class))
        )
    })
    public Mono<ResponseEntity<com.hyperswitch.common.dto.PaymentListFiltersResponse>> getPaymentFilters(
            @RequestHeader("X-Merchant-Id") String merchantId) {
        return paymentService.getPaymentFilters(merchantId)
                .map(result -> {
                    if (result.isOk()) {
                        return ResponseEntity.ok(result.unwrap());
                    } else {
                        throw new PaymentException(result.unwrapErr());
                    }
                });
    }
    
    /**
     * Get payment aggregates
     * GET /api/payments/aggregate
     */
    @GetMapping("/aggregate")
    @Operation(
        summary = "Get payment aggregates",
        description = "Retrieves payment aggregates (status counts) for a merchant within an optional time range"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Aggregates retrieved successfully",
            content = @Content(schema = @Schema(implementation = com.hyperswitch.common.dto.PaymentsAggregateResponse.class))
        )
    })
    public Mono<ResponseEntity<com.hyperswitch.common.dto.PaymentsAggregateResponse>> getPaymentAggregates(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.Instant startTime,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.Instant endTime) {
        return paymentService.getPaymentAggregates(merchantId, startTime, endTime)
                .map(result -> {
                    if (result.isOk()) {
                        return ResponseEntity.ok(result.unwrap());
                    } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get payment methods for a payment
     * GET /api/payments/{paymentId}/payment-methods
     */
    @GetMapping("/{paymentId}/payment-methods")
    @Operation(
        summary = "Get payment methods for payment",
        description = "Retrieves eligible payment methods for a specific payment. Returns saved payment methods for the customer if customer_id was provided when creating the payment."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment methods retrieved successfully"
        )
    })
    public Mono<ResponseEntity<reactor.core.publisher.Flux<com.hyperswitch.common.dto.PaymentMethodResponse>>> getPaymentMethodsForPayment(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @Parameter(description = "Payment ID", required = true)
            @PathVariable("paymentId") String paymentId) {
        return paymentMethodService.getPaymentMethodsForPayment(
                PaymentId.of(paymentId), merchantId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * List refunds with filters
     * POST /api/refunds/list
     */
    @PostMapping("/refunds/list")
    @Operation(
        summary = "List refunds",
        description = "Lists refunds with optional filtering by status, connector, currency, time range, and amount"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Refunds retrieved successfully",
            content = @Content(schema = @Schema(implementation = com.hyperswitch.common.dto.RefundListResponse.class))
        )
    })
    public Mono<ResponseEntity<com.hyperswitch.common.dto.RefundListResponse>> listRefunds(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestBody com.hyperswitch.common.dto.RefundListFilterConstraints constraints) {
        constraints.setMerchantId(merchantId);
        return paymentService.listRefunds(merchantId, constraints)
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
     * GET /api/refunds/filter
     */
    @GetMapping("/refunds/filter")
    @Operation(
        summary = "Get refund filters",
        description = "Returns available filter options for refunds (connectors, currencies, statuses)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = com.hyperswitch.common.dto.RefundFiltersResponse.class))
        )
    })
    public Mono<ResponseEntity<com.hyperswitch.common.dto.RefundFiltersResponse>> getRefundFilters(
            @RequestHeader("X-Merchant-Id") String merchantId) {
        return paymentService.getRefundFilters(merchantId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Sync refund status with connector
     * POST /api/refunds/sync
     */
    @PostMapping("/refunds/sync")
    @Operation(
        summary = "Sync refund",
        description = "Syncs refund status with the connector to get the latest status"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Refund synced successfully",
            content = @Content(schema = @Schema(implementation = Refund.class))
        )
    })
    public Mono<ResponseEntity<Refund>> syncRefund(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestBody com.hyperswitch.common.dto.SyncRefundRequest request) {
        request.setMerchantId(merchantId);
        return paymentService.syncRefund(
                request.getRefundId(),
                request.getPaymentId(),
                merchantId,
                request.getForceSync())
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get refund by ID
     * GET /api/refunds/{id}
     */
    @GetMapping("/refunds/{id}")
    @Operation(
        summary = "Get refund by ID",
        description = "Retrieves a refund by its ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Refund retrieved successfully",
            content = @Content(schema = @Schema(implementation = Refund.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Refund not found"
        )
    })
    public Mono<ResponseEntity<Refund>> getRefund(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @PathVariable("id") String id) {
        return paymentService.getRefund(id, merchantId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Update refund manually
     * PUT /api/refunds/{id}/manual-update
     */
    @PutMapping("/refunds/{id}/manual-update")
    @Operation(
        summary = "Update refund manually",
        description = "Manually updates a refund status or reason"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Refund updated successfully",
            content = @Content(schema = @Schema(implementation = Refund.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Refund not found"
        )
    })
    public Mono<ResponseEntity<Refund>> updateRefund(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @PathVariable String id,
            @RequestBody UpdateRefundRequest request) {
        return paymentService.updateRefund(id, merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get refund aggregates
     * GET /api/refunds/aggregate
     */
    @GetMapping("/refunds/aggregate")
    @Operation(
        summary = "Get refund aggregates",
        description = "Returns refund status counts within a time range"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Refund aggregates retrieved successfully",
            content = @Content(schema = @Schema(implementation = RefundAggregatesResponse.class))
        )
    })
    public Mono<ResponseEntity<RefundAggregatesResponse>> getRefundAggregates(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.Instant endTime) {
        return paymentService.getRefundAggregates(merchantId, startTime, endTime)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Update refund (v1 API)
     * POST /api/refunds/{id}
     */
    @PostMapping("/refunds/{id}")
    @Operation(
        summary = "Update refund (v1)",
        description = "Updates a refund using v1 API. Can update reason and metadata."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Refund updated successfully",
            content = @Content(schema = @Schema(implementation = Refund.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Refund not found"
        )
    })
    public Mono<ResponseEntity<Refund>> updateRefundV1(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @PathVariable String id,
            @RequestBody com.hyperswitch.common.dto.UpdateRefundRequest request) {
        return paymentService.updateRefund(id, merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * List refunds for profile
     * POST /api/refunds/profile/list
     */
    @PostMapping("/refunds/profile/list")
    @Operation(
        summary = "List refunds for profile",
        description = "Lists refunds for a profile with filtering"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Refunds retrieved successfully",
            content = @Content(schema = @Schema(implementation = com.hyperswitch.common.dto.RefundListResponse.class))
        )
    })
    public Mono<ResponseEntity<com.hyperswitch.common.dto.RefundListResponse>> listRefundsForProfile(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestBody com.hyperswitch.common.dto.RefundListFilterConstraints constraints) {
        constraints.setMerchantId(merchantId);
        // Profile listing is similar to regular listing but filtered by profile
        return paymentService.listRefunds(merchantId, constraints)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Filter refunds with POST request
     * POST /api/refunds/filter
     */
    @PostMapping("/refunds/filter")
    @Operation(
        summary = "Filter refunds (POST)",
        description = "Returns available filter options for refunds using POST request"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = com.hyperswitch.common.dto.RefundFiltersResponse.class))
        )
    })
    public Mono<ResponseEntity<com.hyperswitch.common.dto.RefundFiltersResponse>> filterRefunds(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestBody(required = false) java.util.Map<String, Object> request) {
        // POST version of filter - same as GET but allows request body
        return paymentService.getRefundFilters(merchantId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get refund filters (v2 API)
     * GET /api/refunds/v2/filter
     */
    @GetMapping("/refunds/v2/filter")
    @Operation(
        summary = "Get refund filters (v2)",
        description = "Returns available filter options for refunds using v2 API"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = com.hyperswitch.common.dto.RefundFiltersResponse.class))
        )
    })
    public Mono<ResponseEntity<com.hyperswitch.common.dto.RefundFiltersResponse>> getRefundFiltersV2(
            @RequestHeader("X-Merchant-Id") String merchantId) {
        return paymentService.getRefundFilters(merchantId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get refund filters for profile
     * GET /api/refunds/profile/filter
     */
    @GetMapping("/refunds/profile/filter")
    @Operation(
        summary = "Get refund filters for profile",
        description = "Returns available filter options for refunds at profile level"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = com.hyperswitch.common.dto.RefundFiltersResponse.class))
        )
    })
    public Mono<ResponseEntity<com.hyperswitch.common.dto.RefundFiltersResponse>> getRefundFiltersForProfile(
            @RequestHeader("X-Merchant-Id") String merchantId) {
        return paymentService.getRefundFilters(merchantId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get refund filters for profile (v2 API)
     * GET /api/refunds/v2/profile/filter
     */
    @GetMapping("/refunds/v2/profile/filter")
    @Operation(
        summary = "Get refund filters for profile (v2)",
        description = "Returns available filter options for refunds at profile level using v2 API"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = com.hyperswitch.common.dto.RefundFiltersResponse.class))
        )
    })
    public Mono<ResponseEntity<com.hyperswitch.common.dto.RefundFiltersResponse>> getRefundFiltersForProfileV2(
            @RequestHeader("X-Merchant-Id") String merchantId) {
        return paymentService.getRefundFilters(merchantId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get refund aggregates for profile
     * GET /api/refunds/profile/aggregate
     */
    @GetMapping("/refunds/profile/aggregate")
    @Operation(
        summary = "Get refund aggregates for profile",
        description = "Returns refund status counts for a profile within a time range"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Refund aggregates retrieved successfully",
            content = @Content(schema = @Schema(implementation = RefundAggregatesResponse.class))
        )
    })
    public Mono<ResponseEntity<RefundAggregatesResponse>> getRefundAggregatesForProfile(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.Instant endTime) {
        // Profile aggregates are similar to regular aggregates but filtered by profile
        return paymentService.getRefundAggregates(merchantId, startTime, endTime)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get payment by merchant reference ID
     * GET /api/payments/ref/{merchant_reference_id}
     */
    @GetMapping("/ref/{merchant_reference_id}")
    @Operation(
        summary = "Get payment by merchant reference ID",
        description = "Retrieves a payment intent using the merchant's reference ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentIntent.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Payment not found"
        )
    })
    public Mono<ResponseEntity<PaymentIntent>> getPaymentByMerchantReferenceId(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @PathVariable("merchant_reference_id") String merchantReferenceId) {
        return paymentService.getPaymentByMerchantReferenceId(merchantId, merchantReferenceId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get payment recovery list (revenue recovery invoices)
     * GET /api/payments/recovery-list
     */
    @GetMapping("/recovery-list")
    @Operation(
        summary = "Get payment recovery list",
        description = "Returns list of revenue recovery invoices for failed payments"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Recovery list retrieved successfully"
        )
    })
    public Mono<ResponseEntity<reactor.core.publisher.Flux<com.hyperswitch.common.dto.RevenueRecoveryResponse>>> getRecoveryList(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestParam(required = false) com.hyperswitch.common.types.RecoveryStatus status) {
        reactor.core.publisher.Flux<com.hyperswitch.common.dto.RevenueRecoveryResponse> recoveries = 
            revenueRecoveryService.listRecoveries(merchantId, status);
        return Mono.just(ResponseEntity.ok(recoveries));
    }
    
    // ========== Payment Redirect Flows (v1) ==========
    
    /**
     * Start payment redirection (v1)
     * GET /api/payments/redirect/{payment_id}/{merchant_id}/{attempt_id}
     */
    @GetMapping("/redirect/{payment_id}/{merchant_id}/{attempt_id}")
    @Operation(summary = "Start payment redirection (v1)", description = "Starts payment redirection flow (v1)")
    public Mono<ResponseEntity<PaymentIntent>> startPaymentRedirectionV1(
            @PathVariable("payment_id") String paymentId,
            @PathVariable("merchant_id") String merchantId,
            @PathVariable("attempt_id") String attemptId) {
        return paymentService.getPayment(PaymentId.of(paymentId))
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Payment redirect response
     * GET/POST /api/payments/{payment_id}/{merchant_id}/redirect/response/{connector}
     */
    @GetMapping("/{payment_id}/{merchant_id}/redirect/response/{connector}")
    @PostMapping("/{payment_id}/{merchant_id}/redirect/response/{connector}")
    @Operation(summary = "Payment redirect response", description = "Handles payment redirect response from connector")
    public Mono<ResponseEntity<PaymentIntent>> handleRedirectResponse(
            @PathVariable("payment_id") String paymentId,
            @PathVariable("merchant_id") String merchantId,
            @PathVariable("connector") String connector,
            @RequestParam(required = false) java.util.Map<String, String> queryParams) {
        return paymentService.handleRedirectResponse(paymentId, merchantId, connector, queryParams)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Payment redirect response with creds identifier
     * GET/POST /api/payments/{payment_id}/{merchant_id}/redirect/response/{connector}/{creds_identifier}
     */
    @GetMapping("/{payment_id}/{merchant_id}/redirect/response/{connector}/{creds_identifier}")
    @PostMapping("/{payment_id}/{merchant_id}/redirect/response/{connector}/{creds_identifier}")
    @Operation(summary = "Payment redirect response with creds", description = "Handles payment redirect response with credentials identifier")
    public Mono<ResponseEntity<PaymentIntent>> handleRedirectResponseWithCreds(
            @PathVariable("payment_id") String paymentId,
            @PathVariable("merchant_id") String merchantId,
            @PathVariable("connector") String connector,
            @PathVariable("creds_identifier") String credsIdentifier,
            @RequestParam(required = false) java.util.Map<String, String> queryParams) {
        return paymentService.handleRedirectResponseWithCreds(paymentId, merchantId, connector, credsIdentifier, queryParams)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Complete authorization redirect
     * GET/POST /api/payments/{payment_id}/{merchant_id}/redirect/complete/{connector}
     */
    @GetMapping("/{payment_id}/{merchant_id}/redirect/complete/{connector}")
    @PostMapping("/{payment_id}/{merchant_id}/redirect/complete/{connector}")
    @Operation(summary = "Complete authorization redirect", description = "Completes authorization redirect flow")
    public Mono<ResponseEntity<PaymentIntent>> completeAuthorizationRedirect(
            @PathVariable("payment_id") String paymentId,
            @PathVariable("merchant_id") String merchantId,
            @PathVariable("connector") String connector,
            @RequestParam(required = false) java.util.Map<String, String> queryParams) {
        return paymentService.completeAuthorizationRedirect(paymentId, merchantId, connector, queryParams)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Complete authorization redirect with creds identifier
     * GET/POST /api/payments/{payment_id}/{merchant_id}/redirect/complete/{connector}/{creds_identifier}
     */
    @GetMapping("/{payment_id}/{merchant_id}/redirect/complete/{connector}/{creds_identifier}")
    @PostMapping("/{payment_id}/{merchant_id}/redirect/complete/{connector}/{creds_identifier}")
    @Operation(summary = "Complete authorization redirect with creds", description = "Completes authorization redirect with credentials identifier")
    public Mono<ResponseEntity<PaymentIntent>> completeAuthorizationRedirectWithCreds(
            @PathVariable("payment_id") String paymentId,
            @PathVariable("merchant_id") String merchantId,
            @PathVariable("connector") String connector,
            @PathVariable("creds_identifier") String credsIdentifier,
            @RequestParam(required = false) java.util.Map<String, String> queryParams) {
        return paymentService.completeAuthorizationRedirectWithCreds(paymentId, merchantId, connector, credsIdentifier, queryParams)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Complete authorization
     * POST /api/payments/{payment_id}/complete_authorize
     */
    @PostMapping("/{payment_id}/complete_authorize")
    @Operation(summary = "Complete authorization", description = "Completes payment authorization")
    public Mono<ResponseEntity<PaymentIntent>> completeAuthorize(
            @PathVariable("payment_id") String paymentId,
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestBody(required = false) java.util.Map<String, Object> request) {
        return paymentService.completeAuthorize(paymentId, merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    // ========== Payment Status with Gateway Credentials ==========
    
    /**
     * Get payment status with gateway credentials (v1)
     * POST /api/payments/sync
     */
    @PostMapping("/sync")
    @Operation(summary = "Sync payment status (v1)", description = "Gets payment status with gateway credentials (v1)")
    public Mono<ResponseEntity<PaymentIntent>> syncPaymentV1(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestBody com.hyperswitch.common.dto.SyncPaymentRequest request) {
        if (request.getPaymentId() == null) {
            throw new PaymentException(com.hyperswitch.common.errors.PaymentError.of("INVALID_REQUEST", "Payment ID is required"));
        }
        return paymentService.syncPayment(PaymentId.of(request.getPaymentId()), request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    // ========== Payment Manual Update ==========
    
    /**
     * Manually update payment
     * PUT /api/payments/{payment_id}/manual-update
     */
    @PutMapping("/{payment_id}/manual-update")
    @Operation(summary = "Manually update payment", description = "Manually updates payment details")
    public Mono<ResponseEntity<PaymentIntent>> manualUpdatePayment(
            @PathVariable("payment_id") String paymentId,
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestBody java.util.Map<String, Object> updateRequest) {
        return paymentService.manualUpdatePayment(paymentId, merchantId, updateRequest)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    // ========== Payment Metadata Update ==========
    
    /**
     * Update payment metadata
     * POST /api/payments/{payment_id}/update_metadata
     */
    @PostMapping("/{payment_id}/update_metadata")
    @Operation(summary = "Update payment metadata", description = "Updates payment metadata")
    public Mono<ResponseEntity<PaymentIntent>> updatePaymentMetadata(
            @PathVariable("payment_id") String paymentId,
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestBody java.util.Map<String, Object> metadata) {
        return paymentService.updatePaymentMetadata(paymentId, merchantId, metadata)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    // ========== Payment Dynamic Tax Calculation ==========
    
    /**
     * Calculate dynamic tax
     * POST /api/payments/{payment_id}/calculate_tax
     */
    @PostMapping("/{payment_id}/calculate_tax")
    @Operation(summary = "Calculate dynamic tax", description = "Calculates dynamic tax for a payment")
    public Mono<ResponseEntity<com.hyperswitch.common.dto.TaxCalculationResponse>> calculateTax(
            @PathVariable("payment_id") String paymentId,
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestBody com.hyperswitch.common.dto.TaxCalculationRequest request) {
        return paymentService.calculateTax(paymentId, merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    // ========== Payment Extended Card Info ==========
    
    /**
     * Retrieve extended card information
     * GET /api/payments/{payment_id}/extended_card_info
     */
    @GetMapping("/{payment_id}/extended_card_info")
    @Operation(summary = "Get extended card info", description = "Retrieves extended card information for a payment")
    public Mono<ResponseEntity<com.hyperswitch.common.dto.ExtendedCardInfoResponse>> getExtendedCardInfo(
            @PathVariable("payment_id") String paymentId,
            @RequestHeader("X-Merchant-Id") String merchantId) {
        return paymentService.getExtendedCardInfo(paymentId, merchantId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    // ========== Payment Eligibility ==========
    
    /**
     * Check balance and apply payment method data (v2)
     * POST /api/payments/{payment_id}/eligibility/check-balance-and-apply-pm-data
     */
    @PostMapping("/{payment_id}/eligibility/check-balance-and-apply-pm-data")
    @Operation(summary = "Check balance and apply PM data", description = "Checks balance and applies payment method data (v2)")
    public Mono<ResponseEntity<com.hyperswitch.common.dto.EligibilityResponse>> checkBalanceAndApplyPmData(
            @PathVariable("payment_id") String paymentId,
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestBody com.hyperswitch.common.dto.EligibilityRequest request) {
        return paymentService.checkBalanceAndApplyPmData(paymentId, merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Submit eligibility
     * POST /api/payments/{payment_id}/eligibility
     */
    @PostMapping("/{payment_id}/eligibility")
    @Operation(summary = "Submit eligibility", description = "Submits payment eligibility information")
    public Mono<ResponseEntity<com.hyperswitch.common.dto.EligibilityResponse>> submitEligibility(
            @PathVariable("payment_id") String paymentId,
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestBody com.hyperswitch.common.dto.EligibilityRequest request) {
        return paymentService.submitEligibility(paymentId, merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    // ========== Payment Cancel Post Capture ==========
    
    /**
     * Cancel payment after capture
     * POST /api/payments/{payment_id}/cancel_post_capture
     */
    @PostMapping("/{payment_id}/cancel_post_capture")
    @Operation(summary = "Cancel payment post capture", description = "Cancels a payment after it has been captured")
    public Mono<ResponseEntity<PaymentIntent>> cancelPostCapture(
            @PathVariable("payment_id") String paymentId,
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestBody(required = false) com.hyperswitch.common.dto.CancelPostCaptureRequest request) {
        return paymentService.cancelPostCapture(paymentId, merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    // ========== Payment Profile Endpoints ==========
    
    /**
     * List payments for profile
     * GET /api/payments/profile/list
     */
    @GetMapping("/profile/list")
    @Operation(summary = "List payments for profile", description = "Lists payments for a profile")
    public Mono<ResponseEntity<com.hyperswitch.common.dto.PaymentListResponse>> listPaymentsForProfile(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestParam(required = false) String profileId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {
        return paymentService.listPaymentsForProfile(merchantId, profileId, limit, offset)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * List payments for profile with filters
     * POST /api/payments/profile/list
     */
    @PostMapping("/profile/list")
    @Operation(summary = "List payments for profile with filters", description = "Lists payments for a profile with filters")
    public Mono<ResponseEntity<com.hyperswitch.common.dto.PaymentListResponse>> listPaymentsForProfileWithFilters(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestBody com.hyperswitch.common.dto.PaymentListFilterConstraints constraints) {
        return paymentService.listPaymentsForProfileWithFilters(merchantId, constraints)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get payment filters for profile
     * GET /api/payments/profile/filter
     */
    @GetMapping("/profile/filter")
    @Operation(summary = "Get payment filters for profile", description = "Gets available payment filters for a profile")
    public Mono<ResponseEntity<com.hyperswitch.common.dto.PaymentListFiltersResponse>> getPaymentFiltersForProfile(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestParam(required = false) String profileId) {
        return paymentService.getPaymentFiltersForProfile(merchantId, profileId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get payment aggregates for profile
     * GET /api/payments/profile/aggregate
     */
    @GetMapping("/profile/aggregate")
    @Operation(summary = "Get payment aggregates for profile", description = "Gets payment aggregates for a profile")
    public Mono<ResponseEntity<com.hyperswitch.common.dto.PaymentsAggregateResponse>> getPaymentAggregatesForProfile(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestParam(required = false) String profileId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.Instant endTime) {
        return paymentService.getPaymentAggregatesForProfile(merchantId, profileId, startTime, endTime)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
}

