package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.CreatePaymentSessionRequest;
import com.hyperswitch.common.dto.PaymentSessionResponse;
import com.hyperswitch.core.paymentsessions.PaymentSessionService;
import com.hyperswitch.web.controller.PaymentException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST controller for payment sessions (v2 API)
 */
@RestController
@RequestMapping("/api/v2/payment-sessions")
public class PaymentSessionController {

    private static final Logger log = LoggerFactory.getLogger(PaymentSessionController.class);

    private PaymentSessionService sessionService;

    // Default constructor to allow bean creation even if dependencies are missing
    public PaymentSessionController() {
        log.warn("PaymentSessionController created without dependencies - services will be null");
    }

    @Autowired(required = false)
    public void setSessionService(PaymentSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostConstruct
    public void init() {
        log.info("=== PaymentSessionController BEAN CREATED ===");
        log.info("PaymentSessionService available: {}", sessionService != null);
        if (sessionService == null) {
            log.warn("PaymentSessionService is not available - payment session endpoints will not function properly");
        }
    }

    private <T> Mono<ResponseEntity<T>> checkServiceAvailable() {
        if (sessionService == null) {
            return Mono.just(ResponseEntity.status(503)
                .header("X-Error", "PaymentSessionService not available")
                .body(null));
        }
        return null;
    }

    /**
     * Create a payment session
     * POST /api/v2/payment-sessions
     */
    @PostMapping
    public Mono<ResponseEntity<PaymentSessionResponse>> createSession(
            @RequestBody CreatePaymentSessionRequest request) {
        Mono<ResponseEntity<PaymentSessionResponse>> serviceCheck = checkServiceAvailable();
        if (serviceCheck != null) {
            return serviceCheck;
        }
        return sessionService.createSession(request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.status(HttpStatus.CREATED).body(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Get a payment session by session ID
     * GET /api/v2/payment-sessions/{sessionId}
     */
    @GetMapping("/{sessionId}")
    public Mono<ResponseEntity<PaymentSessionResponse>> getSession(
            @PathVariable String sessionId,
            @RequestHeader("X-Merchant-Id") String merchantId) {
        Mono<ResponseEntity<PaymentSessionResponse>> serviceCheck = checkServiceAvailable();
        if (serviceCheck != null) {
            return serviceCheck;
        }
        return sessionService.getSession(sessionId, merchantId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Get a payment session by session token
     * GET /api/v2/payment-sessions/token/{sessionToken}
     */
    @GetMapping("/token/{sessionToken}")
    public Mono<ResponseEntity<PaymentSessionResponse>> getSessionByToken(
            @PathVariable String sessionToken) {
        Mono<ResponseEntity<PaymentSessionResponse>> serviceCheck = checkServiceAvailable();
        if (serviceCheck != null) {
            return serviceCheck;
        }
        return sessionService.getSessionByToken(sessionToken)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Complete a payment session
     * POST /api/v2/payment-sessions/{sessionId}/complete
     */
    @PostMapping("/{sessionId}/complete")
    public Mono<ResponseEntity<PaymentSessionResponse>> completeSession(
            @PathVariable String sessionId,
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestParam String paymentId) {
        Mono<ResponseEntity<PaymentSessionResponse>> serviceCheck = checkServiceAvailable();
        if (serviceCheck != null) {
            return serviceCheck;
        }
        return sessionService.completeSession(sessionId, paymentId, merchantId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }

    /**
     * Cancel a payment session
     * POST /api/v2/payment-sessions/{sessionId}/cancel
     */
    @PostMapping("/{sessionId}/cancel")
    public Mono<ResponseEntity<PaymentSessionResponse>> cancelSession(
            @PathVariable String sessionId,
            @RequestHeader("X-Merchant-Id") String merchantId) {
        Mono<ResponseEntity<PaymentSessionResponse>> serviceCheck = checkServiceAvailable();
        if (serviceCheck != null) {
            return serviceCheck;
        }
        return sessionService.cancelSession(sessionId, merchantId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
}

