package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Request DTO for resuming revenue recovery
 */
public class ResumeRecoveryRequest {
    
    @JsonProperty("revenue_recovery_task")
    private String revenueRecoveryTask;
    
    @JsonProperty("schedule_time")
    private Instant scheduleTime;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("business_status")
    private String businessStatus;
    
    // Getters and Setters
    public String getRevenueRecoveryTask() {
        return revenueRecoveryTask;
    }
    
    public void setRevenueRecoveryTask(String revenueRecoveryTask) {
        this.revenueRecoveryTask = revenueRecoveryTask;
    }
    
    public Instant getScheduleTime() {
        return scheduleTime;
    }
    
    public void setScheduleTime(Instant scheduleTime) {
        this.scheduleTime = scheduleTime;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getBusinessStatus() {
        return businessStatus;
    }
    
    public void setBusinessStatus(String businessStatus) {
        this.businessStatus = businessStatus;
    }
}

