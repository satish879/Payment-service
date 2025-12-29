package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.ApplePayCertificatesMigrationRequest;
import com.hyperswitch.common.dto.ApplePayCertificatesMigrationResponse;
import com.hyperswitch.core.applepay.ApplePayCertificatesMigrationService;
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
 * REST controller for Apple Pay certificates migration
 */
@RestController
@RequestMapping("/api/apple_pay_certificates_migration")
@Tag(name = "Apple Pay Certificates Migration", description = "Apple Pay certificates migration operations")
public class ApplePayCertificatesMigrationController {
    
    private final ApplePayCertificatesMigrationService migrationService;
    
    @Autowired
    public ApplePayCertificatesMigrationController(ApplePayCertificatesMigrationService migrationService) {
        this.migrationService = migrationService;
    }
    
    /**
     * Migrate Apple Pay certificates
     * POST /api/apple_pay_certificates_migration
     */
    @PostMapping
    @Operation(
        summary = "Migrate Apple Pay certificates",
        description = "Migrates Apple Pay certificates for specified merchants by encrypting and moving Apple Pay metadata to connector_wallets_details"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Migration completed",
            content = @Content(schema = @Schema(implementation = ApplePayCertificatesMigrationResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<ApplePayCertificatesMigrationResponse>> migrateApplePayCertificates(
            @RequestBody ApplePayCertificatesMigrationRequest request) {
        return migrationService.migrateApplePayCertificates(request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
}

