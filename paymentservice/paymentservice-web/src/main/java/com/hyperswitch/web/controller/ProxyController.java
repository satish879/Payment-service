package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.ProxyRequest;
import com.hyperswitch.common.dto.ProxyResponse;
import com.hyperswitch.core.proxy.ProxyService;
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
 * REST controller for proxy operations
 */
@RestController
@RequestMapping("/api/proxy")
@Tag(name = "Proxy", description = "Proxy request operations")
public class ProxyController {
    
    private final ProxyService proxyService;
    
    @Autowired
    public ProxyController(ProxyService proxyService) {
        this.proxyService = proxyService;
    }
    
    /**
     * Proxy request
     * POST /api/proxy
     */
    @PostMapping
    @Operation(
        summary = "Proxy request",
        description = "Proxies an HTTP request to a target URL"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Request proxied successfully",
            content = @Content(schema = @Schema(implementation = ProxyResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<ProxyResponse>> proxyRequest(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody ProxyRequest request) {
        return proxyService.proxyRequest(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
}

