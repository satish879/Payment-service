package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Response DTO for backfill status
 */
public class BackfillStatusResponse {
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("processed_count")
    private Integer processedCount;
    
    @JsonProperty("total_count")
    private Integer totalCount;
    
    @JsonProperty("started_at")
    private Instant startedAt;
    
    @JsonProperty("last_updated_at")
    private Instant lastUpdatedAt;
    
    @JsonProperty("error_message")
    private String errorMessage;
    
    // Getters and Setters
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Integer getProcessedCount() {
        return processedCount;
    }
    
    public void setProcessedCount(Integer processedCount) {
        this.processedCount = processedCount;
    }
    
    public Integer getTotalCount() {
        return totalCount;
    }
    
    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }
    
    public Instant getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }
    
    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }
    
    public void setLastUpdatedAt(Instant lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

