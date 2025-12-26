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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * REST controller for profile operations (v1 API)
 */
@RestController
@RequestMapping("/api/account/{account_id}/business_profile")
@Tag(name = "Profiles", description = "Business profile management operations (v1)")
public class ProfileController {
    
    private final ProfileService profileService;
    
    @Autowired
    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }
    
    /**
     * Create profile (v1 API)
     * POST /api/account/{account_id}/business_profile
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
            @PathVariable("account_id") String accountId,
            @RequestBody ProfileRequest request) {
        return profileService.createProfile(accountId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * List profiles (v1 API)
     * GET /api/account/{account_id}/business_profile
     */
    @GetMapping
    @Operation(
        summary = "List business profiles",
        description = "Lists all business profiles for the merchant"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profiles retrieved successfully",
            content = @Content(schema = @Schema(implementation = ProfileResponse.class))
        )
    })
    public Mono<ResponseEntity<Flux<ProfileResponse>>> listProfiles(
            @PathVariable("account_id") String accountId) {
        return profileService.listProfiles(accountId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get profile (v1 API)
     * GET /api/account/{account_id}/business_profile/{profile_id}
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
            @PathVariable("account_id") String accountId,
            @PathVariable("profile_id") String profileId) {
        return profileService.getProfile(accountId, profileId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Update profile (v1 API)
     * POST /api/account/{account_id}/business_profile/{profile_id}
     */
    @PostMapping("/{profile_id}")
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
            @PathVariable("account_id") String accountId,
            @PathVariable("profile_id") String profileId,
            @RequestBody ProfileRequest request) {
        return profileService.updateProfile(accountId, profileId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Delete profile (v1 API)
     * DELETE /api/account/{account_id}/business_profile/{profile_id}
     */
    @DeleteMapping("/{profile_id}")
    @Operation(
        summary = "Delete business profile",
        description = "Deletes a business profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile deleted successfully"
        ),
        @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    public Mono<ResponseEntity<Void>> deleteProfile(
            @PathVariable("account_id") String accountId,
            @PathVariable("profile_id") String profileId) {
        return profileService.deleteProfile(accountId, profileId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok().build();
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Toggle extended card info (v1 API)
     * POST /api/account/{account_id}/business_profile/{profile_id}/toggle_extended_card_info
     */
    @PostMapping("/{profile_id}/toggle_extended_card_info")
    @Operation(
        summary = "Toggle extended card info",
        description = "Toggles the extended card info feature for a profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Extended card info toggled successfully",
            content = @Content(schema = @Schema(implementation = ProfileResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    public Mono<ResponseEntity<ProfileResponse>> toggleExtendedCardInfo(
            @PathVariable("account_id") String accountId,
            @PathVariable("profile_id") String profileId) {
        return profileService.toggleExtendedCardInfo(accountId, profileId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Toggle connector agnostic MIT (v1 API)
     * POST /api/account/{account_id}/business_profile/{profile_id}/toggle_connector_agnostic_mit
     */
    @PostMapping("/{profile_id}/toggle_connector_agnostic_mit")
    @Operation(
        summary = "Toggle connector agnostic MIT",
        description = "Toggles the connector agnostic MIT feature for a profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Connector agnostic MIT toggled successfully",
            content = @Content(schema = @Schema(implementation = ProfileResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    public Mono<ResponseEntity<ProfileResponse>> toggleConnectorAgnosticMit(
            @PathVariable("account_id") String accountId,
            @PathVariable("profile_id") String profileId) {
        return profileService.toggleConnectorAgnosticMit(accountId, profileId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
}

