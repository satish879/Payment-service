package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.PaymentLinkRequest;
import com.hyperswitch.common.dto.PaymentLinkResponse;
import com.hyperswitch.common.types.PaymentLinkId;
import com.hyperswitch.core.paymentlinks.PaymentLinkService;
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
 * REST controller for payment link management
 */
@RestController
@RequestMapping("/api/payment_links")
public class PaymentLinkController {

    private static final Logger log = LoggerFactory.getLogger(PaymentLinkController.class);

    private PaymentLinkService paymentLinkService;

    // Default constructor to allow bean creation even if dependencies are missing
    public PaymentLinkController() {
        log.warn("PaymentLinkController created without dependencies - services will be null");
    }

    @Autowired(required = false)
    public void setPaymentLinkService(PaymentLinkService paymentLinkService) {
        this.paymentLinkService = paymentLinkService;
    }

    @PostConstruct
    public void init() {
        log.info("=== PaymentLinkController BEAN CREATED ===");
        log.info("PaymentLinkService available: {}", paymentLinkService != null);
        if (paymentLinkService == null) {
            log.warn("PaymentLinkService is not available - payment link endpoints will not function properly");
        }
    }


    /**
     * Create a new payment link
     * POST /api/payment_links
     */
    @PostMapping
    public Mono<ResponseEntity<PaymentLinkResponse>> createPaymentLink(@RequestBody PaymentLinkRequest request) {
        if (paymentLinkService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        log.info("Creating payment link for merchant: {}", request.getMerchantId());
        
        return paymentLinkService.createPaymentLink(request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.status(HttpStatus.CREATED).body(result.unwrap());
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                }
            });
    }

    /**
     * Get payment link by ID
     * GET /api/payment_links/{id}
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<PaymentLinkResponse>> getPaymentLink(@PathVariable String id) {
        if (paymentLinkService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        log.info("Getting payment link: {}", id);
        
        return paymentLinkService.getPaymentLink(PaymentLinkId.of(id))
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                }
            });
    }

    /**
     * Get payment link by payment ID
     * GET /api/payment_links/payment/{paymentId}
     */
    @GetMapping("/payment/{paymentId}")
    public Mono<ResponseEntity<PaymentLinkResponse>> getPaymentLinkByPaymentId(@PathVariable String paymentId) {
        if (paymentLinkService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        log.info("Getting payment link by payment ID: {}", paymentId);
        
        return paymentLinkService.getPaymentLinkByPaymentId(paymentId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                }
            });
    }

    /**
     * List payment links for a merchant
     * GET /api/payment_links/merchant/{merchantId}
     */
    @GetMapping("/merchant/{merchantId}")
    public Flux<PaymentLinkResponse> listPaymentLinks(
            @PathVariable String merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (paymentLinkService == null) {
            return Flux.empty();
        }
        log.info("Listing payment links for merchant: {}", merchantId);
        return paymentLinkService.listPaymentLinks(merchantId, page, size);
    }

    /**
     * Validate payment link
     * GET /api/payment_links/{id}/validate
     */
    @GetMapping("/{id}/validate")
    public Mono<ResponseEntity<PaymentLinkResponse>> validatePaymentLink(@PathVariable String id) {
        if (paymentLinkService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        log.info("Validating payment link: {}", id);
        
        return paymentLinkService.validatePaymentLink(PaymentLinkId.of(id))
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                }
            });
    }
}

