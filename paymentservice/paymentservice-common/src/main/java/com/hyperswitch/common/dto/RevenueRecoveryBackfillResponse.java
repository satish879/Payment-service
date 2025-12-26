package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for revenue recovery data backfill
 */
public class RevenueRecoveryBackfillResponse {
    
    @JsonProperty("processed_records")
    private Integer processedRecords;
    
    @JsonProperty("failed_records")
    private Integer failedRecords;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("message")
    private String message;
    
    // Getters and Setters
    public Integer getProcessedRecords() {
        return processedRecords;
    }
    
    public void setProcessedRecords(Integer processedRecords) {
        this.processedRecords = processedRecords;
    }
    
    public Integer getFailedRecords() {
        return failedRecords;
    }
    
    public void setFailedRecords(Integer failedRecords) {
        this.failedRecords = failedRecords;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}

