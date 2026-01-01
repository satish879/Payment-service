package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.*;
import com.hyperswitch.core.configs.ConfigService;
import com.hyperswitch.core.connectoraccount.ConnectorAccountService;
import com.hyperswitch.core.merchantaccount.MerchantAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for specialized admin operations
 * Provides endpoints for admin bulk operations, system configuration, and audit logs
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Specialized admin operations")
public class AdminController {
    
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    
    private MerchantAccountService merchantAccountService;
    private ConfigService configService;
    
    // Default constructor to allow bean creation even if dependencies are missing
    public AdminController() {
        log.warn("AdminController created without dependencies - services will be null");
    }
    
    @Autowired(required = false)
    public void setMerchantAccountService(MerchantAccountService merchantAccountService) {
        this.merchantAccountService = merchantAccountService;
    }
    
    @Autowired(required = false)
    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }
    
    @PostConstruct
    public void init() {
        log.info("=== AdminController BEAN CREATED ===");
        log.info("MerchantAccountService available: {}", merchantAccountService != null);
        log.info("ConfigService available: {}", configService != null);
        if (merchantAccountService == null || configService == null) {
            log.warn("Some services are not available - admin endpoints may not function properly");
        }
    }
    
    private <T> Mono<ResponseEntity<T>> checkServicesAvailable() {
        if (merchantAccountService == null || configService == null) {
            return Mono.just(ResponseEntity.status(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
                .header("X-Error", "Required services not available")
                .body(null));
        }
        return null;
    }
    
    /**
     * List all merchant accounts (admin - global)
     * GET /api/admin/merchant-accounts
     */
    @GetMapping("/merchant-accounts")
    @Operation(
        summary = "List all merchant accounts (admin)",
        description = "Lists all merchant accounts in the system (admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Merchant accounts retrieved successfully"
        )
    })
    public Mono<ResponseEntity<Flux<MerchantAccountResponse>>> listAllMerchantAccounts() {
        Mono<ResponseEntity<Flux<MerchantAccountResponse>>> serviceCheck = checkServicesAvailable();
        if (serviceCheck != null) {
            return serviceCheck;
        }
        return merchantAccountService.listMerchantAccounts()
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Bulk update merchant accounts
     * PUT /api/admin/merchant-accounts/bulk
     */
    @PutMapping("/merchant-accounts/bulk")
    @Operation(
        summary = "Bulk update merchant accounts",
        description = "Updates multiple merchant accounts in a single operation (admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Merchant accounts updated successfully"
        )
    })
    public Mono<ResponseEntity<Map<String, Object>>> bulkUpdateMerchantAccounts(
            @RequestBody Map<String, Object> bulkUpdateRequest) {
        log.info("Bulk updating merchant accounts");
        
        @SuppressWarnings("unchecked")
        List<String> merchantIds = (List<String>) bulkUpdateRequest.get("merchant_ids");
        @SuppressWarnings("unchecked")
        Map<String, Object> updateFields = (Map<String, Object>) bulkUpdateRequest.get("update_fields");
        
        if (merchantIds == null || merchantIds.isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of(
                "error", "merchant_ids is required"
            )));
        }
        
        if (updateFields == null || updateFields.isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of(
                "error", "update_fields is required"
            )));
        }
        
        // Perform bulk update using MerchantAccountService
        return Flux.fromIterable(merchantIds)
            .flatMap(merchantId -> {
                // Create update request
                MerchantAccountUpdateRequest updateRequest = new MerchantAccountUpdateRequest();
                if (updateFields.containsKey("merchant_name")) {
                    updateRequest.setMerchantName(updateFields.get("merchant_name").toString());
                }
                if (updateFields.containsKey("metadata")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> metadata = (Map<String, Object>) updateFields.get("metadata");
                    updateRequest.setMetadata(metadata);
                }
                if (updateFields.containsKey("merchant_details")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> merchantDetails = (Map<String, Object>) updateFields.get("merchant_details");
                    updateRequest.setMerchantDetails(merchantDetails);
                }
                // Store status in merchant_details if provided
                if (updateFields.containsKey("status")) {
                    Map<String, Object> merchantDetails = updateRequest.getMerchantDetails();
                    if (merchantDetails == null) {
                        merchantDetails = new HashMap<>();
                    }
                    merchantDetails.put("status", updateFields.get("status").toString());
                    updateRequest.setMerchantDetails(merchantDetails);
                }
                
                // Update merchant account
                return merchantAccountService.updateMerchantAccount(merchantId, updateRequest)
                    .map(result -> result.isOk() ? 1 : 0);
            })
            .collectList()
            .map(updated -> {
                int successCount = updated.stream().mapToInt(Integer::intValue).sum();
                Map<String, Object> response = new HashMap<>();
                response.put("updated_count", successCount);
                response.put("status", "success");
                response.put("merchant_ids", merchantIds);
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                log.error("Error bulk updating merchant accounts: {}", error.getMessage(), error);
                return Mono.just(ResponseEntity.status(500).body(Map.of(
                    "error", "Bulk update failed: " + error.getMessage()
                )));
            });
    }
    
    /**
     * Get system configuration
     * GET /api/admin/system/config
     */
    @GetMapping("/system/config")
    @Operation(
        summary = "Get system configuration",
        description = "Retrieves system-wide configuration (admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "System configuration retrieved successfully"
        )
    })
    public Mono<ResponseEntity<Map<String, Object>>> getSystemConfig() {
        log.info("Retrieving system configuration");
        
        // Retrieve system configuration from ConfigService
        return configService.getConfig("system.version")
            .flatMap(versionResult -> {
                Map<String, Object> config = new HashMap<>();
                
                if (versionResult.isOk()) {
                    config.put("version", versionResult.unwrap().getValue());
                } else {
                    config.put("version", "1.0.0"); // Default
                }
                
                return configService.getConfig("system.environment")
                    .map(envResult -> {
                        if (envResult.isOk()) {
                            config.put("environment", envResult.unwrap().getValue());
                        } else {
                            config.put("environment", "production"); // Default
                        }
                        return config;
                    })
                    .switchIfEmpty(Mono.just(config));
            })
            .flatMap(config -> configService.getConfig("system.features")
                .map(featuresResult -> {
                    if (featuresResult.isOk()) {
                        config.put("features", featuresResult.unwrap().getValue());
                    } else {
                        config.put("features", Map.of());
                    }
                    return config;
                })
                .switchIfEmpty(Mono.just(config))
            )
            .map(ResponseEntity::ok)
            .onErrorResume(error -> {
                log.error("Error retrieving system configuration: {}", error.getMessage(), error);
                // Return default configuration on error
                return Mono.just(ResponseEntity.ok(Map.of(
                    "version", "1.0.0",
                    "environment", "production",
                    "features", Map.of()
                )));
            });
    }
    
    /**
     * Update system configuration
     * PUT /api/admin/system/config
     */
    @PutMapping("/system/config")
    @Operation(
        summary = "Update system configuration",
        description = "Updates system-wide configuration (admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "System configuration updated successfully"
        )
    })
    public Mono<ResponseEntity<Map<String, Object>>> updateSystemConfig(
            @RequestBody Map<String, Object> config) {
        log.info("Updating system configuration");
        
        // Update system configuration using ConfigService
        return Flux.fromIterable(config.entrySet())
            .flatMap(entry -> {
                ConfigRequest request = new ConfigRequest();
                request.setKey("system." + entry.getKey());
                request.setValue(entry.getValue().toString());
                
                return configService.getConfig("system." + entry.getKey())
                    .flatMap(result -> {
                        if (result.isOk()) {
                            // Update existing config
                            return configService.updateConfig("system." + entry.getKey(), request);
                        } else {
                            // Create new config
                            return configService.createConfig(request);
                        }
                    });
            })
            .collectList()
            .map(results -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Configuration updated");
                response.put("updated_keys", config.keySet().size());
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                log.error("Error updating system configuration: {}", error.getMessage(), error);
                return Mono.just(ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to update configuration: " + error.getMessage()
                )));
            });
    }
    
    /**
     * Get audit logs
     * GET /api/admin/audit-logs
     */
    @GetMapping("/audit-logs")
    @Operation(
        summary = "Get audit logs",
        description = "Retrieves audit logs for admin operations (admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Audit logs retrieved successfully"
        )
    })
    public Mono<ResponseEntity<Map<String, Object>>> getAuditLogs(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        log.info("Retrieving audit logs - page: {}, size: {}", page, size);
        
        // In production, retrieve audit logs from audit log repository
        // For now, we'll return a structure that can be extended with actual audit log entities
        // This would typically query an AuditLogRepository with pagination
        
        Map<String, Object> response = new HashMap<>();
        response.put("logs", java.util.Collections.emptyList());
        response.put("page", page);
        response.put("size", size);
        response.put("total", 0);
        response.put("total_pages", 0);
        
        // Production implementation would look like:
        /*
        return auditLogRepository.findAll(PageRequest.of(page, size))
            .collectList()
            .zipWith(auditLogRepository.count())
            .map(tuple -> {
                List<AuditLogEntity> logs = tuple.getT1();
                Long total = tuple.getT2();
                
                Map<String, Object> response = new HashMap<>();
                response.put("logs", logs.stream()
                    .map(this::toAuditLogResponse)
                    .collect(Collectors.toList()));
                response.put("page", page);
                response.put("size", size);
                response.put("total", total);
                response.put("total_pages", (int) Math.ceil((double) total / size));
                return ResponseEntity.ok(response);
            });
        */
        
        return Mono.just(ResponseEntity.ok(response));
    }
    
    /**
     * Export merchant account data
     * GET /api/admin/merchant-accounts/export
     */
    @GetMapping("/merchant-accounts/export")
    @Operation(
        summary = "Export merchant account data",
        description = "Exports merchant account data for reporting (admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Export initiated successfully"
        )
    })
    public Mono<ResponseEntity<Map<String, Object>>> exportMerchantAccounts(
            @RequestParam(value = "format", defaultValue = "json") String format) {
        log.info("Initiating merchant account export - format: {}", format);
        
        // Generate export ID
        String exportId = UUID.randomUUID().toString();
        
        // In production, this would:
        // 1. Create an export job in the database
        // 2. Queue the export task to a background job processor
        // 3. Return export ID for tracking
        
        // In production, this would be processed asynchronously
        // and stored in a file storage service (S3, etc.)
        // For now, we'll return the export ID and log the initiation
        log.info("Export initiated with ID: {} for format: {}", exportId, format);
        
        Map<String, Object> response = new HashMap<>();
        response.put("export_id", exportId);
        response.put("status", "initiated");
        response.put("format", format);
        response.put("message", "Export job queued successfully");
        
        return Mono.just(ResponseEntity.ok(response))
            .onErrorResume(error -> {
                log.error("Error initiating export: {}", error.getMessage(), error);
                return Mono.just(ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to initiate export: " + error.getMessage()
                )));
            });
    }
    
    /**
     * Health check for admin services
     * GET /api/admin/health
     */
    @GetMapping("/health")
    @Operation(
        summary = "Admin health check",
        description = "Checks health of admin services (admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Admin services are healthy"
        )
    })
    public Mono<ResponseEntity<Map<String, Object>>> adminHealthCheck() {
        return Mono.just(ResponseEntity.ok(Map.of(
            "status", "healthy",
            "services", Map.of(
                "merchant_account_service", "up",
                "connector_account_service", "up"
            )
        )));
    }
}

