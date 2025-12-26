package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.LockerMigrationRequest;
import com.hyperswitch.common.dto.LockerMigrationResponse;
import com.hyperswitch.core.lockermigration.LockerMigrationService;
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
 * REST controller for locker migration operations
 */
@RestController
@RequestMapping("/api/locker_migration")
@Tag(name = "Locker Migration", description = "Locker migration operations")
public class LockerMigrationController {
    
    private final LockerMigrationService lockerMigrationService;
    
    @Autowired
    public LockerMigrationController(LockerMigrationService lockerMigrationService) {
        this.lockerMigrationService = lockerMigrationService;
    }
    
    /**
     * Migrate locker data
     * POST /api/locker_migration/{merchant_id}
     */
    @PostMapping("/{merchant_id}")
    @Operation(
        summary = "Migrate locker data",
        description = "Migrates locker data from old locker to new locker system"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Locker migration started successfully",
            content = @Content(schema = @Schema(implementation = LockerMigrationResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<LockerMigrationResponse>> migrateLocker(
            @PathVariable("merchant_id") String merchantId,
            @RequestBody LockerMigrationRequest request) {
        return lockerMigrationService.migrateLocker(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
}

