package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.ForexConvertResponse;
import com.hyperswitch.common.dto.ForexRatesResponse;
import com.hyperswitch.core.forex.ForexService;
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
 * REST controller for forex/currency operations
 */
@RestController
@RequestMapping("/api/forex")
@Tag(name = "Forex", description = "Forex and currency conversion operations")
public class ForexController {
    
    private final ForexService forexService;
    
    @Autowired
    public ForexController(ForexService forexService) {
        this.forexService = forexService;
    }
    
    /**
     * Get forex rates
     * GET /api/forex/rates
     */
    @GetMapping("/rates")
    @Operation(
        summary = "Get forex rates",
        description = "Retrieves current forex exchange rates"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Forex rates retrieved successfully",
            content = @Content(schema = @Schema(implementation = ForexRatesResponse.class))
        )
    })
    public Mono<ResponseEntity<ForexRatesResponse>> getForexRates(
            @RequestParam(value = "base_currency", required = false, defaultValue = "USD") String baseCurrency) {
        return forexService.getForexRates(baseCurrency)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Convert from minor currency units
     * GET /api/forex/convert_from_minor
     */
    @GetMapping("/convert_from_minor")
    @Operation(
        summary = "Convert from minor currency units",
        description = "Converts an amount from one currency to another (from minor units)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Currency converted successfully",
            content = @Content(schema = @Schema(implementation = ForexConvertResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<ForexConvertResponse>> convertFromMinor(
            @RequestParam("amount") Long amount,
            @RequestParam("from_currency") String fromCurrency,
            @RequestParam("to_currency") String toCurrency) {
        return forexService.convertFromMinor(amount, fromCurrency, toCurrency)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
}

