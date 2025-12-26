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
 * REST controller for API key operations (v1 API)
 */
@RestController
@RequestMapping("/api/api_keys")
@Tag(name = "API Keys", description = "API key management operations (v1)")
public class ApiKeyController {
    
    private final ApiKeyService apiKeyService;
    
    @Autowired
    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }
    
    /**
     * Create API key (v1 API)
     * POST /api/api_keys/{merchant_id}
     */
    @PostMapping("/{merchant_id}")
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
            @PathVariable("merchant_id") String merchantId,
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
     * List API keys (v1 API)
     * GET /api/api_keys/{merchant_id}/list
     */
    @GetMapping("/{merchant_id}/list")
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
            @PathVariable("merchant_id") String merchantId) {
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
     * Get API key (v1 API)
     * GET /api/api_keys/{merchant_id}/{key_id}
     */
    @GetMapping("/{merchant_id}/{key_id}")
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
            @PathVariable("merchant_id") String merchantId,
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
     * Update API key (v1 API)
     * POST /api/api_keys/{merchant_id}/{key_id}
     */
    @PostMapping("/{merchant_id}/{key_id}")
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
            @PathVariable("merchant_id") String merchantId,
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
     * Revoke API key (v1 API)
     * DELETE /api/api_keys/{merchant_id}/{key_id}
     */
    @DeleteMapping("/{merchant_id}/{key_id}")
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
            @PathVariable("merchant_id") String merchantId,
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

