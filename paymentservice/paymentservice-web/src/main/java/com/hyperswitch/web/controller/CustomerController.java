package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.CustomerRequest;
import com.hyperswitch.common.dto.CustomerResponse;
import com.hyperswitch.web.controller.PaymentException;
import com.hyperswitch.common.types.CustomerId;
import com.hyperswitch.common.types.MerchantId;
import com.hyperswitch.core.customers.CustomerService;
import com.hyperswitch.core.mandates.MandateService;
import com.hyperswitch.core.paymentmethods.PaymentMethodService;
import com.hyperswitch.common.dto.PaymentMethodResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller for customer management
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

    private CustomerService customerService;
    private MandateService mandateService;
    private PaymentMethodService paymentMethodService;

    // Default constructor to allow bean creation even if dependencies are missing
    public CustomerController() {
        log.warn("CustomerController created without dependencies - services will be null");
    }

    @Autowired(required = false)
    public void setCustomerService(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Autowired(required = false)
    public void setMandateService(MandateService mandateService) {
        this.mandateService = mandateService;
    }

    @Autowired(required = false)
    public void setPaymentMethodService(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    @PostConstruct
    public void init() {
        log.info("=== CustomerController BEAN CREATED ===");
        log.info("CustomerService available: {}", customerService != null);
        log.info("MandateService available: {}", mandateService != null);
        log.info("PaymentMethodService available: {}", paymentMethodService != null);
        if (customerService == null) {
            log.warn("CustomerService is not available - customer endpoints will not function properly");
        }
    }

    private <T> Mono<ResponseEntity<T>> checkServiceAvailable() {
        if (customerService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("X-Error", "CustomerService not available")
                .body(null));
        }
        return null;
    }

    /**
     * Create a new customer
     * POST /api/customers
     */
    @PostMapping
    public Mono<ResponseEntity<CustomerResponse>> createCustomer(
            @RequestHeader(value = "X-Merchant-Id", required = false) String merchantIdHeader,
            @RequestBody CustomerRequest request) {
        Mono<ResponseEntity<CustomerResponse>> serviceCheck = checkServiceAvailable();
        if (serviceCheck != null) {
            return serviceCheck;
        }
        // Set merchantId from header if not already set in request body
        if (merchantIdHeader != null && (request.getMerchantId() == null || request.getMerchantId().getValue() == null)) {
            request.setMerchantId(MerchantId.of(merchantIdHeader));
        }
        return customerService.createCustomer(request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.status(HttpStatus.CREATED).body(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Get customer by ID
     * GET /api/customers/{id}
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<CustomerResponse>> getCustomer(@PathVariable String id) {
        Mono<ResponseEntity<CustomerResponse>> serviceCheck = checkServiceAvailable();
        if (serviceCheck != null) {
            return serviceCheck;
        }
        CustomerId customerId = CustomerId.of(id);
        return customerService.getCustomer(customerId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Update customer
     * POST /api/customers/{id}
     */
    @PostMapping("/{id}")
    public Mono<ResponseEntity<CustomerResponse>> updateCustomer(
            @PathVariable String id,
            @RequestHeader(value = "X-Merchant-Id", required = false) String merchantIdHeader,
            @RequestBody CustomerRequest request) {
        CustomerId customerId = CustomerId.of(id);
        // Set merchantId from header if not already set in request body
        if (merchantIdHeader != null && (request.getMerchantId() == null || request.getMerchantId().getValue() == null)) {
            request.setMerchantId(MerchantId.of(merchantIdHeader));
        }
        return customerService.updateCustomer(customerId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Delete customer
     * DELETE /api/customers/{id}
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteCustomer(@PathVariable String id) {
        CustomerId customerId = CustomerId.of(id);
        return customerService.deleteCustomer(customerId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.noContent().build();
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * List customers for a merchant
     * GET /api/customers?merchant_id={merchantId}&page={page}&size={size}
     */
    @GetMapping
    public Mono<ResponseEntity<Flux<CustomerResponse>>> listCustomers(
            @RequestHeader(value = "X-Merchant-Id", required = false) String merchantIdHeader,
            @RequestParam(value = "merchant_id", required = false) String merchantIdParam,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Prefer header over query parameter
        String merchantIdStr = merchantIdHeader != null ? merchantIdHeader : merchantIdParam;
        if (merchantIdStr == null) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        MerchantId merchantId = MerchantId.of(merchantIdStr);
        Pageable pageable = PageRequest.of(page, size);
        
        return customerService.listCustomers(merchantId, pageable)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * List customers with count
     * GET /api/customers/list_with_count
     */
    @GetMapping("/list_with_count")
    public Mono<ResponseEntity<com.hyperswitch.common.dto.CustomerListWithCountResponse>> listCustomersWithCount(
            @RequestParam("merchant_id") String merchantIdParam,
            @RequestParam(defaultValue = "100") Integer limit,
            @RequestParam(defaultValue = "0") Integer offset) {
        MerchantId merchantId = MerchantId.of(merchantIdParam);
        return customerService.listCustomersWithCount(merchantId, limit, offset)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get total payment method count for a merchant
     * GET /api/customers/total-payment-methods
     */
    @GetMapping("/total-payment-methods")
    public Mono<ResponseEntity<Long>> getTotalPaymentMethodCount(
            @RequestParam("merchant_id") String merchantIdParam) {
        MerchantId merchantId = MerchantId.of(merchantIdParam);
        return customerService.getTotalPaymentMethodCount(merchantId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * List mandates for a customer
     * GET /api/customers/{id}/mandates
     */
    @GetMapping("/{id}/mandates")
    public Flux<com.hyperswitch.common.dto.MandateResponse> getCustomerMandates(
            @PathVariable String id) {
        return mandateService.listCustomerMandates(id);
    }
    
    /**
     * List customer saved payment methods (v2 API)
     * GET /api/v2/customers/{customer_id}/saved-payment-methods
     */
    @GetMapping("/v2/customers/{customer_id}/saved-payment-methods")
    @Operation(
        summary = "List customer saved payment methods (v2)",
        description = "List the payment methods saved for a customer using v2 API"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment methods retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Customer not found"
        )
    })
    public Mono<ResponseEntity<Flux<PaymentMethodResponse>>> listCustomerSavedPaymentMethodsV2(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @Parameter(description = "The unique identifier for the customer", required = true)
            @PathVariable("customer_id") String customerId) {
        return paymentMethodService.listCustomerPaymentMethods(com.hyperswitch.common.types.CustomerId.of(customerId))
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get total payment method count (v2 API)
     * GET /api/v2/customers/total-payment-methods
     */
    @GetMapping("/v2/customers/total-payment-methods")
    @Operation(
        summary = "Get total payment method count (v2)",
        description = "Get total count of payment methods for a merchant using v2 API"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Total payment method count retrieved successfully"
        )
    })
    public Mono<ResponseEntity<Long>> getTotalPaymentMethodCountV2(
            @RequestHeader("X-Merchant-Id") String merchantId) {
        return customerService.getTotalPaymentMethodCount(com.hyperswitch.common.types.MerchantId.of(merchantId))
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
}

