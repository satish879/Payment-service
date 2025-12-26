package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.ProfileAcquirerRequest;
import com.hyperswitch.common.dto.ProfileAcquirerResponse;
import com.hyperswitch.core.profileacquirer.ProfileAcquirerService;
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
 * REST controller for profile acquirer operations
 */
@RestController
@RequestMapping("/api/profile_acquirer")
@Tag(name = "Profile Acquirer", description = "Profile acquirer management operations")
public class ProfileAcquirerController {
    
    private final ProfileAcquirerService profileAcquirerService;
    
    @Autowired
    public ProfileAcquirerController(ProfileAcquirerService profileAcquirerService) {
        this.profileAcquirerService = profileAcquirerService;
    }
    
    /**
     * Create profile acquirer
     * POST /api/profile_acquirer
     */
    @PostMapping
    @Operation(
        summary = "Create profile acquirer",
        description = "Creates a new profile acquirer configuration"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile acquirer created successfully",
            content = @Content(schema = @Schema(implementation = ProfileAcquirerResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<ProfileAcquirerResponse>> createProfileAcquirer(
            @RequestHeader("merchant_id") String merchantId,
            @RequestParam("profile_id") String profileId,
            @RequestBody ProfileAcquirerRequest request) {
        return profileAcquirerService.createProfileAcquirer(merchantId, profileId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Update profile acquirer
     * POST /api/profile_acquirer/{profile_id}/{profile_acquirer_id}
     */
    @PostMapping("/{profile_id}/{profile_acquirer_id}")
    @Operation(
        summary = "Update profile acquirer",
        description = "Updates an existing profile acquirer configuration"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile acquirer updated successfully",
            content = @Content(schema = @Schema(implementation = ProfileAcquirerResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Profile acquirer not found")
    })
    public Mono<ResponseEntity<ProfileAcquirerResponse>> updateProfileAcquirer(
            @RequestHeader("merchant_id") String merchantId,
            @PathVariable("profile_id") String profileId,
            @PathVariable("profile_acquirer_id") String profileAcquirerId,
            @RequestBody ProfileAcquirerRequest request) {
        return profileAcquirerService.updateProfileAcquirer(merchantId, profileId, profileAcquirerId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
}

