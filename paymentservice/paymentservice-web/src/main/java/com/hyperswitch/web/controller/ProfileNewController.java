package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.ConnectorAccountResponse;
import com.hyperswitch.common.dto.ProfileResponse;
import com.hyperswitch.core.connectoraccount.ConnectorAccountService;
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

/**
 * REST controller for Profile New operations (v1 API)
 * Provides endpoints for listing profiles at profile level and listing connectors for a profile
 */
@RestController
@RequestMapping("/api/account/{account_id}/profile")
@Tag(name = "Profile New", description = "Profile operations at profile level (v1)")
public class ProfileNewController {
    
    private final ProfileService profileService;
    private final ConnectorAccountService connectorAccountService;
    
    @Autowired
    public ProfileNewController(
            ProfileService profileService,
            ConnectorAccountService connectorAccountService) {
        this.profileService = profileService;
        this.connectorAccountService = connectorAccountService;
    }
    
    /**
     * List profiles at profile level
     * GET /api/account/{account_id}/profile
     */
    @GetMapping
    @Operation(
        summary = "List profiles at profile level",
        description = "Lists profiles filtered by the profile ID from authentication context"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profiles retrieved successfully",
            content = @Content(schema = @Schema(implementation = ProfileResponse.class))
        )
    })
    public Mono<ResponseEntity<Flux<ProfileResponse>>> listProfilesAtProfileLevel(
            @PathVariable("account_id") String accountId,
            @RequestHeader(value = "X-Profile-Id", required = false) String profileId) {
        
        // In production, profileId would come from authentication context
        // For now, we use the header or default to accountId
        String effectiveProfileId = profileId != null ? profileId : accountId;
        
        return profileService.listProfilesAtProfileLevel(accountId, effectiveProfileId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * List connectors for profile
     * GET /api/account/{account_id}/profile/connectors
     */
    @GetMapping("/connectors")
    @Operation(
        summary = "List connectors for profile",
        description = "Lists all connector accounts associated with the profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Connector accounts retrieved successfully",
            content = @Content(schema = @Schema(implementation = ConnectorAccountResponse.class))
        )
    })
    public Mono<ResponseEntity<Flux<ConnectorAccountResponse>>> listConnectorsForProfile(
            @PathVariable("account_id") String accountId,
            @RequestHeader(value = "X-Profile-Id", required = false) String profileId) {
        
        // In production, profileId would come from authentication context
        // For now, we use the header or default to accountId
        String effectiveProfileId = profileId != null ? profileId : accountId;
        
        return connectorAccountService.listConnectorAccountsForProfile(accountId, effectiveProfileId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
}

