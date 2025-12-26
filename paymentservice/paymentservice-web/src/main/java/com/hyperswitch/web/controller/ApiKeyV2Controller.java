package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.ApiKeyRequest;
import com.hyperswitch.common.dto.ApiKeyResponse;
import com.hyperswitch.core.apikeys.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller for API key operations (v2 API)
 */
@RestController
@RequestMapping("/api/v2/api-keys")
@Tag(name = "API Keys V2", description = "API key management operations (v2)")
public class ApiKeyV2Controller {
    
    private final ApiKeyService apiKeyService;
    
    @Autowired
    public ApiKeyV2Controller(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }
    
    /**
     * Create API key (v2 API)
     * POST /api/v2/api-keys
     */
    @PostMapping
    @Operation(
        summary = "Create API key",
        description = "Creates a new API key for the merchant"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "API key created successfully",
            content = @Content(schema = @Schema(implementation = ApiKeyResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<ApiKeyResponse>> createApiKey(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody ApiKeyRequest request) {
        return apiKeyService.createApiKey(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * List API keys (v2 API)
     * GET /api/v2/api-keys/list
     */
    @GetMapping("/list")
    @Operation(
        summary = "List API keys",
        description = "Lists all API keys for the merchant"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "API keys retrieved successfully",
            content = @Content(schema = @Schema(implementation = ApiKeyResponse.class))
        )
    })
    public Mono<ResponseEntity<Flux<ApiKeyResponse>>> listApiKeys(
            @RequestHeader("merchant_id") String merchantId) {
        return apiKeyService.listApiKeys(merchantId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get API key (v2 API)
     * GET /api/v2/api-keys/{key_id}
     */
    @GetMapping("/{key_id}")
    @Operation(
        summary = "Get API key",
        description = "Retrieves an API key by its ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "API key retrieved successfully",
            content = @Content(schema = @Schema(implementation = ApiKeyResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "API key not found")
    })
    public Mono<ResponseEntity<ApiKeyResponse>> getApiKey(
            @RequestHeader("merchant_id") String merchantId,
            @PathVariable("key_id") String keyId) {
        return apiKeyService.getApiKey(merchantId, keyId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Update API key (v2 API)
     * PUT /api/v2/api-keys/{key_id}
     */
    @PutMapping("/{key_id}")
    @Operation(
        summary = "Update API key",
        description = "Updates an existing API key"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "API key updated successfully",
            content = @Content(schema = @Schema(implementation = ApiKeyResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "API key not found")
    })
    public Mono<ResponseEntity<ApiKeyResponse>> updateApiKey(
            @RequestHeader("merchant_id") String merchantId,
            @PathVariable("key_id") String keyId,
            @RequestBody ApiKeyRequest request) {
        return apiKeyService.updateApiKey(merchantId, keyId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Revoke API key (v2 API)
     * DELETE /api/v2/api-keys/{key_id}
     */
    @DeleteMapping("/{key_id}")
    @Operation(
        summary = "Revoke API key",
        description = "Revokes an API key"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "API key revoked successfully"
        ),
        @ApiResponse(responseCode = "404", description = "API key not found")
    })
    public Mono<ResponseEntity<Void>> revokeApiKey(
            @RequestHeader("merchant_id") String merchantId,
            @PathVariable("key_id") String keyId) {
        return apiKeyService.revokeApiKey(merchantId, keyId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok().build();
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
}

