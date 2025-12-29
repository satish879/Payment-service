package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.*;
import com.hyperswitch.core.dummyconnector.DummyConnectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST controller for Dummy Connector operations (testing)
 */
@RestController
@RequestMapping("/api/dummy-connector")
@Tag(name = "Dummy Connector", description = "Dummy connector operations for testing (v1)")
public class DummyConnectorController {
    
    private final DummyConnectorService dummyConnectorService;
    
    @Autowired
    public DummyConnectorController(DummyConnectorService dummyConnectorService) {
        this.dummyConnectorService = dummyConnectorService;
    }
    
    /**
     * Create dummy payment
     * POST /api/dummy-connector/payment
     */
    @PostMapping("/payment")
    @Operation(
        summary = "Create dummy payment",
        description = "Creates a dummy payment for testing purposes"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Dummy payment created successfully",
            content = @Content(schema = @Schema(implementation = DummyConnectorPaymentResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<DummyConnectorPaymentResponse>> createPayment(
            @RequestBody DummyConnectorPaymentRequest request) {
        return dummyConnectorService.createPayment(request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get dummy payment data
     * GET /api/dummy-connector/payments/{payment_id}
     */
    @GetMapping("/payments/{payment_id}")
    @Operation(
        summary = "Get dummy payment data",
        description = "Retrieves dummy payment data by payment ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment data retrieved successfully",
            content = @Content(schema = @Schema(implementation = DummyConnectorPaymentResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public Mono<ResponseEntity<DummyConnectorPaymentResponse>> getPaymentData(
            @PathVariable("payment_id") String paymentId) {
        return dummyConnectorService.getPaymentData(paymentId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Create dummy refund
     * POST /api/dummy-connector/payments/{payment_id}/refund
     */
    @PostMapping("/payments/{payment_id}/refund")
    @Operation(
        summary = "Create dummy refund",
        description = "Creates a dummy refund for a payment"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Dummy refund created successfully",
            content = @Content(schema = @Schema(implementation = DummyConnectorRefundResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<DummyConnectorRefundResponse>> createRefund(
            @PathVariable("payment_id") String paymentId,
            @RequestBody DummyConnectorRefundRequest request) {
        return dummyConnectorService.createRefund(paymentId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get dummy refund data
     * GET /api/dummy-connector/refunds/{refund_id}
     */
    @GetMapping("/refunds/{refund_id}")
    @Operation(
        summary = "Get dummy refund data",
        description = "Retrieves dummy refund data by refund ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Refund data retrieved successfully",
            content = @Content(schema = @Schema(implementation = DummyConnectorRefundResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Refund not found")
    })
    public Mono<ResponseEntity<DummyConnectorRefundResponse>> getRefundData(
            @PathVariable("refund_id") String refundId) {
        return dummyConnectorService.getRefundData(refundId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Authorize dummy payment
     * GET /api/dummy-connector/authorize/{attempt_id}
     */
    @GetMapping("/authorize/{attempt_id}")
    @Operation(
        summary = "Authorize dummy payment",
        description = "Returns authorization page for 3DS flow"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Authorization page returned"
        ),
        @ApiResponse(responseCode = "404", description = "Payment attempt not found")
    })
    public Mono<ResponseEntity<String>> authorizePayment(
            @PathVariable("attempt_id") String attemptId) {
        return dummyConnectorService.authorizePayment(attemptId)
            .map(result -> {
                if (result.isOk()) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.TEXT_HTML);
                    return ResponseEntity.ok().headers(headers).body(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Complete dummy payment
     * GET /api/dummy-connector/complete/{attempt_id}
     */
    @GetMapping("/complete/{attempt_id}")
    @Operation(
        summary = "Complete dummy payment",
        description = "Completes a dummy payment after authorization"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment completed successfully"
        ),
        @ApiResponse(responseCode = "404", description = "Payment attempt not found")
    })
    public Mono<ResponseEntity<Void>> completePayment(
            @PathVariable("attempt_id") String attemptId,
            @RequestParam(value = "confirm", defaultValue = "true") Boolean confirm) {
        return dummyConnectorService.completePayment(attemptId, confirm)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok().build();
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
}

