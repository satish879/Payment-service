package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Response DTO for locker migration
 */
public class LockerMigrationResponse {
    
    @JsonProperty("merchant_id")
    private String merchantId;
    
    @JsonProperty("migration_id")
    private String migrationId;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("migrated_count")
    private Integer migratedCount;
    
    @JsonProperty("failed_count")
    private Integer failedCount;
    
    @JsonProperty("started_at")
    private Instant startedAt;
    
    @JsonProperty("completed_at")
    private Instant completedAt;
    
    @JsonProperty("message")
    private String message;
    
    // Getters and Setters
    public String getMerchantId() {
        return merchantId;
    }
    
    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }
    
    public String getMigrationId() {
        return migrationId;
    }
    
    public void setMigrationId(String migrationId) {
        this.migrationId = migrationId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Integer getMigratedCount() {
        return migratedCount;
    }
    
    public void setMigratedCount(Integer migratedCount) {
        this.migratedCount = migratedCount;
    }
    
    public Integer getFailedCount() {
        return failedCount;
    }
    
    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }
    
    public Instant getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }
    
    public Instant getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}

