package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.NetworkTokenStatusResponse;
import com.hyperswitch.common.dto.PaymentMethodRequest;
import com.hyperswitch.common.dto.PaymentMethodResponse;
import com.hyperswitch.web.controller.PaymentException;
import com.hyperswitch.common.types.CustomerId;
import com.hyperswitch.common.types.PaymentMethodId;
import com.hyperswitch.core.paymentmethods.PaymentMethodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller for payment method management
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Payment Methods", description = "Payment method management operations including creation, retrieval, update, and network token status checks")
public class PaymentMethodController {

    private static final Logger log = LoggerFactory.getLogger(PaymentMethodController.class);

    private PaymentMethodService paymentMethodService;

    // Constructor injection for required dependencies
    @Autowired
    public PaymentMethodController(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
        log.info("PaymentMethodController created with PaymentMethodService: {}", paymentMethodService != null ? "OK" : "NULL");
    }

    @PostConstruct
    public void init() {
        log.info("=== PaymentMethodController BEAN CREATED ===");
        log.info("PaymentMethodService available: {}", paymentMethodService != null);
        if (paymentMethodService == null) {
            log.warn("PaymentMethodService is not available - payment method endpoints will not function properly");
        }
    }

    /**
     * Create a new payment method
     * POST /api/payment_methods
     */
    @Operation(
        summary = "Create a payment method",
        description = "Creates a new payment method for a customer. Supports various payment method types including cards, wallets, and bank accounts.",
        requestBody = @RequestBody(
            description = "Payment method creation request",
            required = true,
            content = @Content(
                examples = {
                    @ExampleObject(
                        name = "Card Payment Method",
                        summary = "Example: Card payment method",
                        value = """
                            {
                              "customerId": "cust_123",
                              "merchantId": "merchant_123",
                              "paymentMethodType": "CARD",
                              "paymentMethodData": {
                                "cardNumber": "4242424242424242",
                                "expiryMonth": 12,
                                "expiryYear": 2025,
                                "cvc": "123"
                              }
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
            description = "Payment method created successfully",
            content = @Content(schema = @Schema(implementation = PaymentMethodResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content = @Content(schema = @Schema(implementation = com.hyperswitch.common.dto.ErrorResponse.class))
        )
    })
    @PostMapping("/payment_methods")
    public Mono<ResponseEntity<PaymentMethodResponse>> createPaymentMethod(
            @RequestHeader(value = "X-Merchant-Id", required = false) String merchantIdHeader,
            @Parameter(description = "Payment method creation request", required = true)
            @RequestBody PaymentMethodRequest request) {
        if (paymentMethodService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        // Set merchantId from header if not already set in request body
        if (merchantIdHeader != null && (request.getMerchantId() == null || request.getMerchantId().getValue() == null)) {
            request.setMerchantId(com.hyperswitch.common.types.MerchantId.of(merchantIdHeader));
        }
        return paymentMethodService.createPaymentMethod(request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.status(HttpStatus.CREATED).body(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Get payment method by ID
     * GET /api/payment_methods/{id}
     */
    @GetMapping("/payment_methods/{id}")
    @Operation(summary = "Get payment method", description = "Retrieves a payment method by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment method found"),
        @ApiResponse(responseCode = "404", description = "Payment method not found")
    })
    public Mono<ResponseEntity<PaymentMethodResponse>> getPaymentMethod(
            @Parameter(description = "Payment method ID", required = true)
            @PathVariable String id) {
        if (paymentMethodService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        PaymentMethodId paymentMethodId = PaymentMethodId.of(id);
        return paymentMethodService.getPaymentMethod(paymentMethodId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * List payment methods for a customer
     * GET /api/customers/{customerId}/payment_methods
     */
    @GetMapping("/customers/{customerId}/payment_methods")
    public Flux<PaymentMethodResponse> listCustomerPaymentMethods(
            @PathVariable String customerId) {
        if (paymentMethodService == null) {
            return Flux.error(new RuntimeException("PaymentMethodService not available"));
        }
        CustomerId customerIdObj = CustomerId.of(customerId);
        return paymentMethodService.listCustomerPaymentMethods(customerIdObj)
            .flatMapMany(result -> {
                if (result.isOk()) {
                    Flux<PaymentMethodResponse> responseFlux = result.unwrap();
                    log.debug("Successfully retrieved payment methods for customer: {}", customerId);
                    return responseFlux;
                } else {
                    log.error("Error retrieving payment methods for customer {}: {}", customerId, result.unwrapErr());
                    return Flux.error(new PaymentException(result.unwrapErr()));
                }
            })
            .onErrorResume(throwable -> {
                log.error("Unexpected error in listCustomerPaymentMethods for customer {}: {}", customerId, throwable.getMessage(), throwable);
                return Flux.error(new PaymentException(com.hyperswitch.common.errors.PaymentError.of("INTERNAL_ERROR", "Failed to retrieve payment methods")));
            });
    }

    /**
     * Set default payment method for a customer
     * POST /api/customers/{customerId}/payment_methods/{paymentMethodId}/default
     */
    @PostMapping("/customers/{customerId}/payment_methods/{paymentMethodId}/default")
    public Mono<ResponseEntity<Void>> setDefaultPaymentMethod(
            @PathVariable String customerId,
            @PathVariable String paymentMethodId) {
        if (paymentMethodService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        CustomerId customerIdObj = CustomerId.of(customerId);
        PaymentMethodId paymentMethodIdObj = PaymentMethodId.of(paymentMethodId);
        return paymentMethodService.setDefaultPaymentMethod(customerIdObj, paymentMethodIdObj)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok().build();
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Delete payment method
     * DELETE /api/payment_methods/{id}
     */
    @DeleteMapping("/payment_methods/{id}")
    public Mono<ResponseEntity<Void>> deletePaymentMethod(@PathVariable String id) {
        if (paymentMethodService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        PaymentMethodId paymentMethodId = PaymentMethodId.of(id);
        return paymentMethodService.deletePaymentMethod(paymentMethodId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.noContent().build();
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Get payment method by client secret
     * GET /api/payment_methods/client_secret?client_secret={clientSecret}
     */
    @GetMapping("/payment_methods/client_secret")
    @Operation(
        summary = "Get payment method by client secret",
        description = "Retrieves a payment method using its client secret. Used for client-side payment method retrieval."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment method found"),
        @ApiResponse(responseCode = "404", description = "Payment method not found for client secret")
    })
    public Mono<ResponseEntity<PaymentMethodResponse>> getPaymentMethodByClientSecret(
            @Parameter(description = "Client secret", required = true)
            @RequestParam("client_secret") String clientSecret) {
        if (paymentMethodService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        return paymentMethodService.getPaymentMethodByClientSecret(clientSecret)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Update saved payment method
     * PUT /api/payment_methods/{id}/update-saved-payment-method
     */
    @PutMapping("/payment_methods/{id}/update-saved-payment-method")
    @Operation(
        summary = "Update saved payment method",
        description = "Updates an existing saved payment method. Can update payment method data, network transaction ID, and connector mandate details."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment method updated successfully"),
        @ApiResponse(responseCode = "404", description = "Payment method not found")
    })
    public Mono<ResponseEntity<PaymentMethodResponse>> updateSavedPaymentMethod(
            @Parameter(description = "Payment method ID", required = true)
            @PathVariable String id,
            @Parameter(description = "Payment method update request", required = true)
            @RequestBody PaymentMethodRequest request) {
        if (paymentMethodService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        PaymentMethodId paymentMethodId = PaymentMethodId.of(id);
        return paymentMethodService.updatePaymentMethod(paymentMethodId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Check network token status
     * GET /api/payment_methods/{id}/check-network-token-status
     */
    @GetMapping("/payment_methods/{id}/check-network-token-status")
    @Operation(
        summary = "Check network token status",
        description = "Checks the network token status for a payment method. Returns whether the payment method has an active network token and its status."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Network token status retrieved",
            content = @Content(schema = @Schema(implementation = NetworkTokenStatusResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Payment method not found")
    })
    public Mono<ResponseEntity<NetworkTokenStatusResponse>> checkNetworkTokenStatus(
            @Parameter(description = "Payment method ID", required = true)
            @PathVariable String id) {
        if (paymentMethodService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        PaymentMethodId paymentMethodId = PaymentMethodId.of(id);
        return paymentMethodService.checkNetworkTokenStatus(paymentMethodId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Tokenize a card
     * POST /api/payment_methods/tokenize-card
     */
    @PostMapping("/payment_methods/tokenize-card")
    @Operation(
        summary = "Tokenize a card",
        description = "Tokenizes a card and creates a payment method. Optionally enables network tokenization."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Card tokenized successfully",
            content = @Content(schema = @Schema(implementation = com.hyperswitch.common.dto.TokenizeCardResponse.class))
        )
    })
    public Mono<ResponseEntity<com.hyperswitch.common.dto.TokenizeCardResponse>> tokenizeCard(
            @Parameter(description = "Card tokenization request", required = true)
            @RequestBody com.hyperswitch.common.dto.TokenizeCardRequest request) {
        if (paymentMethodService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        return paymentMethodService.tokenizeCard(request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.status(HttpStatus.CREATED).body(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * List payment methods
     * GET /api/payment_methods
     */
    @GetMapping("/payment_methods")
    @Operation(
        summary = "List payment methods",
        description = "Lists payment methods with optional filtering by merchant, customer, and payment method type"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment methods retrieved successfully"
        )
    })
    public Mono<ResponseEntity<Flux<PaymentMethodResponse>>> listPaymentMethods(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String paymentMethodType) {
        if (paymentMethodService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        return paymentMethodService.listPaymentMethods(merchantId, customerId, paymentMethodType)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get payment method token
     * GET /api/payment_methods/{id}/get-token
     */
    @GetMapping("/payment_methods/{id}/get-token")
    @Operation(
        summary = "Get payment method token",
        description = "Retrieves token data for a payment method including network token if available"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token retrieved successfully",
            content = @Content(schema = @Schema(implementation = com.hyperswitch.common.dto.PaymentMethodTokenResponse.class))
        )
    })
    public Mono<ResponseEntity<com.hyperswitch.common.dto.PaymentMethodTokenResponse>> getPaymentMethodToken(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @Parameter(description = "Payment method ID", required = true)
            @PathVariable String id) {
        if (paymentMethodService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        return paymentMethodService.getPaymentMethodToken(
                com.hyperswitch.common.types.PaymentMethodId.of(id), merchantId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get payment method filters
     * GET /api/payment_methods/filter
     */
    @GetMapping("/filter")
    @Operation(
        summary = "Get payment method filters",
        description = "Retrieves available payment method filters by connector, including supported currencies and countries"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Filters retrieved successfully",
            content = @Content(schema = @Schema(implementation = com.hyperswitch.common.dto.PaymentMethodFilterResponse.class))
        )
    })
    public Mono<ResponseEntity<com.hyperswitch.common.dto.PaymentMethodFilterResponse>> getPaymentMethodFilters(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestParam(required = false) String connector) {
        if (paymentMethodService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        return paymentMethodService.getPaymentMethodFilters(merchantId, connector)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Migrate payment method from one connector to another
     * POST /api/payment_methods/migrate
     */
    @PostMapping("/migrate")
    @Operation(
        summary = "Migrate payment method",
        description = "Migrates a payment method from one connector to another."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment method migrated successfully",
            content = @Content(schema = @Schema(implementation = PaymentMethodResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Payment method not found"
        )
    })
    public Mono<ResponseEntity<PaymentMethodResponse>> migratePaymentMethod(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestBody com.hyperswitch.common.dto.PaymentMethodMigrateRequest request) {
        if (paymentMethodService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        return paymentMethodService.migratePaymentMethod(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Batch migrate payment methods
     * POST /api/payment_methods/migrate-batch
     */
    @PostMapping("/migrate-batch")
    @Operation(
        summary = "Batch migrate payment methods",
        description = "Migrates multiple payment methods from one connector to another in a single operation."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Batch migration completed",
            content = @Content(schema = @Schema(implementation = com.hyperswitch.common.dto.PaymentMethodBatchMigrateResponse.class))
        )
    })
    public Mono<ResponseEntity<com.hyperswitch.common.dto.PaymentMethodBatchMigrateResponse>> batchMigratePaymentMethods(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestBody com.hyperswitch.common.dto.PaymentMethodBatchMigrateRequest request) {
        if (paymentMethodService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        return paymentMethodService.batchMigratePaymentMethods(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Batch update payment methods
     * POST /api/payment_methods/update-batch
     */
    @PostMapping("/update-batch")
    @Operation(
        summary = "Batch update payment methods",
        description = "Updates multiple payment methods in a single operation."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Batch update completed",
            content = @Content(schema = @Schema(implementation = com.hyperswitch.common.dto.PaymentMethodBatchUpdateResponse.class))
        )
    })
    public Mono<ResponseEntity<com.hyperswitch.common.dto.PaymentMethodBatchUpdateResponse>> batchUpdatePaymentMethods(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestBody com.hyperswitch.common.dto.PaymentMethodBatchUpdateRequest request) {
        if (paymentMethodService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        return paymentMethodService.batchUpdatePaymentMethods(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Batch tokenize cards
     * POST /api/payment_methods/tokenize-card-batch
     */
    @PostMapping("/tokenize-card-batch")
    @Operation(
        summary = "Batch tokenize cards",
        description = "Tokenizes multiple cards in a single operation."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Batch tokenization completed",
            content = @Content(schema = @Schema(implementation = com.hyperswitch.common.dto.BatchTokenizeCardResponse.class))
        )
    })
    public Mono<ResponseEntity<com.hyperswitch.common.dto.BatchTokenizeCardResponse>> batchTokenizeCards(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestBody com.hyperswitch.common.dto.BatchTokenizeCardRequest request) {
        if (paymentMethodService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        return paymentMethodService.batchTokenizeCards(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Initiate payment method collect link
     * POST /api/payment_methods/collect
     */
    @PostMapping("/collect")
    @Operation(
        summary = "Initiate payment method collect link",
        description = "Generates a form link for collecting payment methods for a customer."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Collect link created successfully",
            content = @Content(schema = @Schema(implementation = com.hyperswitch.common.dto.PaymentMethodCollectLinkResponse.class))
        )
    })
    public Mono<ResponseEntity<com.hyperswitch.common.dto.PaymentMethodCollectLinkResponse>> initiatePaymentMethodCollectLink(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestBody com.hyperswitch.common.dto.PaymentMethodCollectLinkRequest request) {
        if (paymentMethodService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        return paymentMethodService.initiatePaymentMethodCollectLink(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Render payment method collect link
     * GET /api/payment_methods/collect/{merchant_id}/{pm_collect_link_id}
     */
    @GetMapping("/collect/{merchant_id}/{pm_collect_link_id}")
    @Operation(
        summary = "Render payment method collect link",
        description = "Renders the payment method collection form for the specified collect link."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Collect link form rendered successfully",
            content = @Content(mediaType = "text/html")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Collect link not found"
        )
    })
    public Mono<ResponseEntity<String>> renderPaymentMethodCollectLink(
            @Parameter(description = "Merchant ID", required = true)
            @PathVariable("merchant_id") String merchantId,
            @Parameter(description = "Collect link ID", required = true)
            @PathVariable("pm_collect_link_id") String collectLinkId) {
        if (paymentMethodService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        return paymentMethodService.renderPaymentMethodCollectLink(merchantId, collectLinkId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok()
                        .header("Content-Type", "text/html")
                        .body(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Create payment method intent (v2 API)
     * POST /api/v2/payment-methods/create-intent
     */
    @PostMapping("/v2/payment-methods/create-intent")
    @Operation(
        summary = "Create payment method intent",
        description = "Creates a payment method intent for deferred payment method creation. The payment method will be in 'intent_created' status until confirmed."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment method intent created successfully",
            content = @Content(schema = @Schema(implementation = PaymentMethodResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request"
        )
    })
    public Mono<ResponseEntity<PaymentMethodResponse>> createPaymentMethodIntent(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestBody com.hyperswitch.common.dto.PaymentMethodIntentCreateRequest request) {
        if (paymentMethodService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        return paymentMethodService.createPaymentMethodIntent(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Confirm payment method intent (v2 API)
     * POST /api/v2/payment-methods/{id}/confirm-intent
     */
    @PostMapping("/v2/payment-methods/{id}/confirm-intent")
    @Operation(
        summary = "Confirm payment method intent",
        description = "Confirms a payment method intent by providing the actual payment method data. The payment method status will change from 'intent_created' to 'active'."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment method intent confirmed successfully",
            content = @Content(schema = @Schema(implementation = PaymentMethodResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Payment method intent not found"
        )
    })
    public Mono<ResponseEntity<PaymentMethodResponse>> confirmPaymentMethodIntent(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @Parameter(description = "Payment method ID", required = true)
            @PathVariable("id") String paymentMethodId,
            @RequestBody com.hyperswitch.common.dto.PaymentMethodIntentConfirmRequest request) {
        if (paymentMethodService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        return paymentMethodService.confirmPaymentMethodIntent(merchantId, paymentMethodId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Tokenize card using existing payment method
     * POST /api/payment_methods/{payment_method_id}/tokenize-card
     */
    @PostMapping("/{payment_method_id}/tokenize-card")
    @Operation(
        summary = "Tokenize card using existing payment method",
        description = "Tokenizes a card using an existing payment method as a base"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Card tokenized successfully",
            content = @Content(schema = @Schema(implementation = com.hyperswitch.common.dto.TokenizeCardResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Payment method not found"
        )
    })
    public Mono<ResponseEntity<com.hyperswitch.common.dto.TokenizeCardResponse>> tokenizeCardUsingPaymentMethod(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @Parameter(description = "Payment method ID", required = true)
            @PathVariable("payment_method_id") String paymentMethodId,
            @RequestBody com.hyperswitch.common.dto.TokenizeCardRequest request) {
        if (paymentMethodService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        return paymentMethodService.tokenizeCardUsingPaymentMethod(
                merchantId, 
                com.hyperswitch.common.types.PaymentMethodId.of(paymentMethodId),
                request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Update payment method (v1 alternative endpoint)
     * POST /api/payment_methods/{payment_method_id}/update
     */
    @PostMapping("/{payment_method_id}/update")
    @Operation(
        summary = "Update payment method (v1)",
        description = "Updates a payment method using the v1 API endpoint"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment method updated successfully",
            content = @Content(schema = @Schema(implementation = PaymentMethodResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Payment method not found"
        )
    })
    public Mono<ResponseEntity<PaymentMethodResponse>> updatePaymentMethodV1(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @Parameter(description = "Payment method ID", required = true)
            @PathVariable("payment_method_id") String paymentMethodId,
            @RequestBody PaymentMethodRequest request) {
        if (paymentMethodService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        return paymentMethodService.updatePaymentMethodV1(
                merchantId,
                com.hyperswitch.common.types.PaymentMethodId.of(paymentMethodId),
                request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Save payment method
     * POST /api/payment_methods/{payment_method_id}/save
     */
    @PostMapping("/{payment_method_id}/save")
    @Operation(
        summary = "Save payment method",
        description = "Saves a payment method for future use"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment method saved successfully",
            content = @Content(schema = @Schema(implementation = PaymentMethodResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Payment method not found"
        )
    })
    public Mono<ResponseEntity<PaymentMethodResponse>> savePaymentMethod(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @Parameter(description = "Payment method ID", required = true)
            @PathVariable("payment_method_id") String paymentMethodId) {
        if (paymentMethodService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        return paymentMethodService.savePaymentMethod(
                merchantId,
                com.hyperswitch.common.types.PaymentMethodId.of(paymentMethodId))
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Create payment method auth link
     * POST /api/payment_methods/auth/link
     */
    @PostMapping("/auth/link")
    @Operation(
        summary = "Create payment method auth link",
        description = "Creates a link token for payment method authentication"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Link token created successfully",
            content = @Content(schema = @Schema(implementation = com.hyperswitch.common.dto.PaymentMethodAuthLinkResponse.class))
        )
    })
    public Mono<ResponseEntity<com.hyperswitch.common.dto.PaymentMethodAuthLinkResponse>> createPaymentMethodAuthLink(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestBody com.hyperswitch.common.dto.PaymentMethodAuthLinkRequest request) {
        if (paymentMethodService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        return paymentMethodService.createPaymentMethodAuthLink(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Exchange payment method auth token
     * POST /api/payment_methods/auth/exchange
     */
    @PostMapping("/auth/exchange")
    @Operation(
        summary = "Exchange payment method auth token",
        description = "Exchanges a public token for an access token for payment method authentication"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token exchanged successfully",
            content = @Content(schema = @Schema(implementation = com.hyperswitch.common.dto.PaymentMethodAuthExchangeResponse.class))
        )
    })
    public Mono<ResponseEntity<com.hyperswitch.common.dto.PaymentMethodAuthExchangeResponse>> exchangePaymentMethodAuthToken(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestBody com.hyperswitch.common.dto.PaymentMethodAuthExchangeRequest request) {
        if (paymentMethodService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        return paymentMethodService.exchangePaymentMethodAuthToken(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Get payment method token data (v2 API)
     * POST /api/v2/payment-methods/{payment_method_id}/get-token-data
     */
    @PostMapping("/v2/payment-methods/{payment_method_id}/get-token-data")
    @Operation(
        summary = "Get payment method token data (v2)",
        description = "Get token data for a payment method using v2 API with OLAP support"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token data retrieved successfully",
            content = @Content(schema = @Schema(implementation = com.hyperswitch.common.dto.TokenDataResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Payment method not found"
        )
    })
    public Mono<ResponseEntity<com.hyperswitch.common.dto.TokenDataResponse>> getPaymentMethodTokenData(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @Parameter(description = "Payment method ID", required = true)
            @PathVariable("payment_method_id") String paymentMethodId,
            @RequestBody com.hyperswitch.common.dto.GetTokenDataRequest request) {
        if (paymentMethodService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        return paymentMethodService.getPaymentMethodTokenData(
                merchantId,
                com.hyperswitch.common.types.PaymentMethodId.of(paymentMethodId),
                request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
}

