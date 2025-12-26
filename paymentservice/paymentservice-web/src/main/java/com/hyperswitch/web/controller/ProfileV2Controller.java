package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.ProfileRequest;
import com.hyperswitch.common.dto.ProfileResponse;
import com.hyperswitch.core.profiles.ProfileService;
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

import java.util.Map;

/**
 * REST controller for profile operations (v2 API)
 */
@RestController
@RequestMapping("/api/v2/profiles")
@Tag(name = "Profiles V2", description = "Business profile management operations (v2)")
public class ProfileV2Controller {
    
    private final ProfileService profileService;
    
    @Autowired
    public ProfileV2Controller(ProfileService profileService) {
        this.profileService = profileService;
    }
    
    /**
     * Create profile (v2 API)
     * POST /api/v2/profiles
     */
    @PostMapping
    @Operation(
        summary = "Create business profile",
        description = "Creates a new business profile for the merchant"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile created successfully",
            content = @Content(schema = @Schema(implementation = ProfileResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<ProfileResponse>> createProfile(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody ProfileRequest request) {
        return profileService.createProfile(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get profile (v2 API)
     * GET /api/v2/profiles/{profile_id}
     */
    @GetMapping("/{profile_id}")
    @Operation(
        summary = "Get business profile",
        description = "Retrieves a business profile by its ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile retrieved successfully",
            content = @Content(schema = @Schema(implementation = ProfileResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    public Mono<ResponseEntity<ProfileResponse>> getProfile(
            @RequestHeader("merchant_id") String merchantId,
            @PathVariable("profile_id") String profileId) {
        return profileService.getProfile(merchantId, profileId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Update profile (v2 API)
     * PUT /api/v2/profiles/{profile_id}
     */
    @PutMapping("/{profile_id}")
    @Operation(
        summary = "Update business profile",
        description = "Updates an existing business profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile updated successfully",
            content = @Content(schema = @Schema(implementation = ProfileResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    public Mono<ResponseEntity<ProfileResponse>> updateProfile(
            @RequestHeader("merchant_id") String merchantId,
            @PathVariable("profile_id") String profileId,
            @RequestBody ProfileRequest request) {
        return profileService.updateProfile(merchantId, profileId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get fallback routing (v2 API)
     * GET /api/v2/profiles/{profile_id}/fallback-routing
     */
    @GetMapping("/{profile_id}/fallback-routing")
    @Operation(
        summary = "Get fallback routing",
        description = "Retrieves the fallback routing configuration for a profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Fallback routing retrieved successfully"
        ),
        @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    public Mono<ResponseEntity<Map<String, Object>>> getFallbackRouting(
            @RequestHeader("merchant_id") String merchantId,
            @PathVariable("profile_id") String profileId) {
        return profileService.getFallbackRouting(merchantId, profileId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Update fallback routing (v2 API)
     * PATCH /api/v2/profiles/{profile_id}/fallback-routing
     */
    @PatchMapping("/{profile_id}/fallback-routing")
    @Operation(
        summary = "Update fallback routing",
        description = "Updates the fallback routing configuration for a profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Fallback routing updated successfully"
        ),
        @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    public Mono<ResponseEntity<Map<String, Object>>> updateFallbackRouting(
            @RequestHeader("merchant_id") String merchantId,
            @PathVariable("profile_id") String profileId,
            @RequestBody Map<String, Object> fallbackRouting) {
        return profileService.updateFallbackRouting(merchantId, profileId, fallbackRouting)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Activate routing algorithm (v2 API)
     * PATCH /api/v2/profiles/{profile_id}/activate-routing-algorithm
     */
    @PatchMapping("/{profile_id}/activate-routing-algorithm")
    @Operation(
        summary = "Activate routing algorithm",
        description = "Activates a routing algorithm for a profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Routing algorithm activated successfully",
            content = @Content(schema = @Schema(implementation = ProfileResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    public Mono<ResponseEntity<ProfileResponse>> activateRoutingAlgorithm(
            @RequestHeader("merchant_id") String merchantId,
            @PathVariable("profile_id") String profileId,
            @RequestBody Map<String, String> request) {
        String algorithmId = request.get("algorithm_id");
        return profileService.activateRoutingAlgorithm(merchantId, profileId, algorithmId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Deactivate routing algorithm (v2 API)
     * PATCH /api/v2/profiles/{profile_id}/deactivate-routing-algorithm
     */
    @PatchMapping("/{profile_id}/deactivate-routing-algorithm")
    @Operation(
        summary = "Deactivate routing algorithm",
        description = "Deactivates the routing algorithm for a profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Routing algorithm deactivated successfully",
            content = @Content(schema = @Schema(implementation = ProfileResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    public Mono<ResponseEntity<ProfileResponse>> deactivateRoutingAlgorithm(
            @RequestHeader("merchant_id") String merchantId,
            @PathVariable("profile_id") String profileId) {
        return profileService.deactivateRoutingAlgorithm(merchantId, profileId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get routing algorithm (v2 API)
     * GET /api/v2/profiles/{profile_id}/routing-algorithm
     */
    @GetMapping("/{profile_id}/routing-algorithm")
    @Operation(
        summary = "Get routing algorithm",
        description = "Retrieves the routing algorithm configuration for a profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Routing algorithm retrieved successfully"
        ),
        @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    public Mono<ResponseEntity<Map<String, Object>>> getRoutingAlgorithm(
            @RequestHeader("merchant_id") String merchantId,
            @PathVariable("profile_id") String profileId) {
        return profileService.getRoutingAlgorithm(merchantId, profileId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Upsert decision manager config (v2 API)
     * PUT /api/v2/profiles/{profile_id}/decision
     */
    @PutMapping("/{profile_id}/decision")
    @Operation(
        summary = "Upsert decision manager config",
        description = "Creates or updates the decision manager configuration for a profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Decision manager config updated successfully"
        ),
        @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    public Mono<ResponseEntity<Map<String, Object>>> upsertDecisionManagerConfig(
            @RequestHeader("merchant_id") String merchantId,
            @PathVariable("profile_id") String profileId,
            @RequestBody Map<String, Object> decisionConfig) {
        return profileService.upsertDecisionManagerConfig(merchantId, profileId, decisionConfig)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get decision manager config (v2 API)
     * GET /api/v2/profiles/{profile_id}/decision
     */
    @GetMapping("/{profile_id}/decision")
    @Operation(
        summary = "Get decision manager config",
        description = "Retrieves the decision manager configuration for a profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Decision manager config retrieved successfully"
        ),
        @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    public Mono<ResponseEntity<Map<String, Object>>> getDecisionManagerConfig(
            @RequestHeader("merchant_id") String merchantId,
            @PathVariable("profile_id") String profileId) {
        return profileService.getDecisionManagerConfig(merchantId, profileId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
}

