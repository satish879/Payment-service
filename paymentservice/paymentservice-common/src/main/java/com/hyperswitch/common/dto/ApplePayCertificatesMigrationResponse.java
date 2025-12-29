package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response DTO for Apple Pay certificates migration
 */
public class ApplePayCertificatesMigrationResponse {
    
    @JsonProperty("migration_successful")
    private List<String> migrationSuccessful;
    
    @JsonProperty("migration_failed")
    private List<String> migrationFailed;
    
    // Getters and Setters
    public List<String> getMigrationSuccessful() {
        return migrationSuccessful;
    }
    
    public void setMigrationSuccessful(List<String> migrationSuccessful) {
        this.migrationSuccessful = migrationSuccessful;
    }
    
    public List<String> getMigrationFailed() {
        return migrationFailed;
    }
    
    public void setMigrationFailed(List<String> migrationFailed) {
        this.migrationFailed = migrationFailed;
    }
}

