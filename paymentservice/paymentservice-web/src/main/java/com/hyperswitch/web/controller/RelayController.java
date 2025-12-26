package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.RelayRequest;
import com.hyperswitch.common.dto.RelayResponse;
import com.hyperswitch.core.relay.RelayService;
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
 * REST controller for relay operations
 */
@RestController
@RequestMapping("/api/relay")
@Tag(name = "Relay", description = "Relay request operations")
public class RelayController {
    
    private final RelayService relayService;
    
    @Autowired
    public RelayController(RelayService relayService) {
        this.relayService = relayService;
    }
    
    /**
     * Create relay request
     * POST /api/relay
     */
    @PostMapping
    @Operation(
        summary = "Create relay request",
        description = "Creates a relay request to forward to a connector"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Relay request created successfully",
            content = @Content(schema = @Schema(implementation = RelayResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<RelayResponse>> createRelay(
            @RequestHeader("merchant_id") String merchantId,
            @RequestHeader("profile_id") String profileId,
            @RequestBody RelayRequest request) {
        return relayService.createRelay(merchantId, profileId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get relay by ID
     * GET /api/relay/{relay_id}
     */
    @GetMapping("/{relay_id}")
    @Operation(
        summary = "Get relay request",
        description = "Retrieves a relay request by its ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Relay request retrieved successfully",
            content = @Content(schema = @Schema(implementation = RelayResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Relay not found")
    })
    public Mono<ResponseEntity<RelayResponse>> getRelay(
            @RequestHeader("merchant_id") String merchantId,
            @PathVariable("relay_id") String relayId) {
        return relayService.getRelay(merchantId, relayId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
}

