package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.BackfillStatusResponse;
import com.hyperswitch.common.dto.RevenueRecoveryBackfillRequest;
import com.hyperswitch.common.dto.RevenueRecoveryBackfillResponse;
import com.hyperswitch.common.dto.RevenueRecoveryRedisResponse;
import com.hyperswitch.common.dto.UpdateTokenRequest;
import com.hyperswitch.core.revenuerecovery.RevenueRecoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * REST controller for recovery data backfill operations (v2 API)
 */
@RestController
@RequestMapping("/api/v2/recovery/data-backfill")
@Tag(name = "Recovery Data Backfill V2", description = "Recovery data backfill operations (v2)")
public class RecoveryDataBackfillV2Controller {
    
    private final RevenueRecoveryService revenueRecoveryService;
    
    @Autowired
    public RecoveryDataBackfillV2Controller(RevenueRecoveryService revenueRecoveryService) {
        this.revenueRecoveryService = revenueRecoveryService;
    }
    
    /**
     * Backfill revenue recovery data
     * POST /api/v2/recovery/data-backfill
     */
    @PostMapping
    @Operation(
        summary = "Backfill revenue recovery data",
        description = "Backfills revenue recovery data from provided records"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Data backfilled successfully",
            content = @Content(schema = @Schema(implementation = RevenueRecoveryBackfillResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<RevenueRecoveryBackfillResponse>> dataBackfill(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody List<RevenueRecoveryBackfillRequest> records,
            @RequestParam(value = "cutoff_datetime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant cutoffDatetime) {
        return revenueRecoveryService.dataBackfill(merchantId, records, cutoffDatetime)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get backfill status
     * POST /api/v2/recovery/data-backfill/status/{connector_customer_id}/{payment_intent_id}
     */
    @PostMapping("/status/{connector_customer_id}/{payment_intent_id}")
    @Operation(
        summary = "Get backfill status",
        description = "Retrieves the status of a revenue recovery data backfill operation"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Backfill status retrieved successfully",
            content = @Content(schema = @Schema(implementation = BackfillStatusResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Backfill not found")
    })
    public Mono<ResponseEntity<BackfillStatusResponse>> getBackfillStatus(
            @RequestHeader("merchant_id") String merchantId,
            @PathVariable("connector_customer_id") String connectorCustomerId,
            @PathVariable("payment_intent_id") String paymentIntentId) {
        return revenueRecoveryService.getBackfillStatusByConnectorCustomer(merchantId, connectorCustomerId, paymentIntentId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get Redis data
     * GET /api/v2/recovery/data-backfill/redis-data/{connector_customer_id}
     */
    @GetMapping("/redis-data/{connector_customer_id}")
    @Operation(
        summary = "Get Redis data",
        description = "Retrieves revenue recovery data from Redis for a connector customer"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Redis data retrieved successfully",
            content = @Content(schema = @Schema(implementation = RevenueRecoveryRedisResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Data not found")
    })
    public Mono<ResponseEntity<RevenueRecoveryRedisResponse>> getRedisData(
            @RequestHeader("merchant_id") String merchantId,
            @PathVariable("connector_customer_id") String connectorCustomerId) {
        return revenueRecoveryService.getRedisDataByConnectorCustomer(merchantId, connectorCustomerId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Update token
     * PUT /api/v2/recovery/data-backfill/update-token
     */
    @PutMapping("/update-token")
    @Operation(
        summary = "Update token",
        description = "Updates a token in the recovery data backfill"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token updated successfully"
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<Void>> updateToken(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody UpdateTokenRequest request) {
        return revenueRecoveryService.updateToken(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok().build();
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
}

