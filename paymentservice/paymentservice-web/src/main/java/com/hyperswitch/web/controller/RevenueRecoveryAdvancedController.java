package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.BackfillStatusResponse;
import com.hyperswitch.common.dto.ProcessTrackerResponse;
import com.hyperswitch.common.dto.ResumeRecoveryRequest;
import com.hyperswitch.common.dto.RevenueRecoveryBackfillRequest;
import com.hyperswitch.common.dto.RevenueRecoveryBackfillResponse;
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
import java.util.Map;

/**
 * REST controller for advanced revenue recovery operations
 */
@RestController
@RequestMapping("/api/revenue_recovery")
@Tag(name = "Revenue Recovery Advanced", description = "Advanced revenue recovery operations")
public class RevenueRecoveryAdvancedController {
    
    private final RevenueRecoveryService revenueRecoveryService;
    
    @Autowired
    public RevenueRecoveryAdvancedController(RevenueRecoveryService revenueRecoveryService) {
        this.revenueRecoveryService = revenueRecoveryService;
    }
    
    /**
     * Backfill revenue recovery data
     * POST /api/revenue_recovery/data_backfill
     */
    @PostMapping("/data_backfill")
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
     * Update Redis data
     * POST /api/revenue_recovery/update_redis_data
     */
    @PostMapping("/update_redis_data")
    @Operation(
        summary = "Update Redis data",
        description = "Updates revenue recovery data in Redis"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Redis data updated successfully"
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<Void>> updateRedisData(
            @RequestHeader("merchant_id") String merchantId,
            @RequestParam("key") String key,
            @RequestBody Map<String, Object> data) {
        return revenueRecoveryService.updateRedisData(merchantId, key, data)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok().build();
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get backfill status
     * GET /api/revenue_recovery/data_backfill_status
     */
    @GetMapping("/data_backfill_status")
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
            @RequestParam("backfill_id") String backfillId) {
        return revenueRecoveryService.getBackfillStatus(merchantId, backfillId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get process tracker data
     * GET /api/revenue_recovery/pt/{process_id}
     */
    @GetMapping("/pt/{process_id}")
    @Operation(
        summary = "Get process tracker data",
        description = "Retrieves process tracker data for a revenue recovery process"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Process tracker data retrieved successfully",
            content = @Content(schema = @Schema(implementation = ProcessTrackerResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Process tracker not found")
    })
    public Mono<ResponseEntity<ProcessTrackerResponse>> getProcessTracker(
            @RequestHeader("merchant_id") String merchantId,
            @PathVariable("process_id") String processId) {
        return revenueRecoveryService.getProcessTracker(merchantId, processId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Resume revenue recovery
     * POST /api/revenue_recovery/resume
     */
    @PostMapping("/resume")
    @Operation(
        summary = "Resume revenue recovery",
        description = "Resumes a revenue recovery process"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Recovery resumed successfully",
            content = @Content(schema = @Schema(implementation = ProcessTrackerResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<ProcessTrackerResponse>> resumeRecovery(
            @RequestHeader("merchant_id") String merchantId,
            @RequestParam("process_id") String processId,
            @RequestBody ResumeRecoveryRequest request) {
        return revenueRecoveryService.resumeRecovery(merchantId, processId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
}

