package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.*;
import com.hyperswitch.core.gsm.GsmService;
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
 * REST controller for GSM (Global Settings Management) operations (v1 API)
 */
@RestController
@RequestMapping("/api/gsm")
@Tag(name = "GSM", description = "Global Settings Management rule operations (v1)")
public class GsmController {
    
    private final GsmService gsmService;
    
    @Autowired
    public GsmController(GsmService gsmService) {
        this.gsmService = gsmService;
    }
    
    /**
     * Create GSM rule
     * POST /api/gsm
     */
    @PostMapping
    @Operation(
        summary = "Create GSM rule",
        description = "Creates a GSM (Global Status Mapping) rule to map connector error codes/messages to unified status"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "GSM rule created successfully",
            content = @Content(schema = @Schema(implementation = GsmResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<GsmResponse>> createGsmRule(
            @RequestBody GsmCreateRequest request) {
        return gsmService.createGsmRule(request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get GSM rule
     * POST /api/gsm/get
     */
    @PostMapping("/get")
    @Operation(
        summary = "Get GSM rule",
        description = "Retrieves a GSM rule by connector, flow, sub_flow, code, and message"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "GSM rule retrieved successfully",
            content = @Content(schema = @Schema(implementation = GsmResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "GSM rule not found")
    })
    public Mono<ResponseEntity<GsmResponse>> getGsmRule(
            @RequestBody GsmRetrieveRequest request) {
        return gsmService.getGsmRule(request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Update GSM rule
     * POST /api/gsm/update
     */
    @PostMapping("/update")
    @Operation(
        summary = "Update GSM rule",
        description = "Updates an existing GSM rule"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "GSM rule updated successfully",
            content = @Content(schema = @Schema(implementation = GsmResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "GSM rule not found")
    })
    public Mono<ResponseEntity<GsmResponse>> updateGsmRule(
            @RequestBody GsmUpdateRequest request) {
        return gsmService.updateGsmRule(request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Delete GSM rule
     * POST /api/gsm/delete
     */
    @PostMapping("/delete")
    @Operation(
        summary = "Delete GSM rule",
        description = "Deletes a GSM rule"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "GSM rule deleted successfully",
            content = @Content(schema = @Schema(implementation = GsmDeleteResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "GSM rule not found")
    })
    public Mono<ResponseEntity<GsmDeleteResponse>> deleteGsmRule(
            @RequestBody GsmDeleteRequest request) {
        return gsmService.deleteGsmRule(request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
}

