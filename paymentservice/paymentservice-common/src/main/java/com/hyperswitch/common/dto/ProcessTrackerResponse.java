package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for process tracker
 */
public class ProcessTrackerResponse {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("tag")
    private List<String> tag;
    
    @JsonProperty("runner")
    private String runner;
    
    @JsonProperty("retry_count")
    private Integer retryCount;
    
    @JsonProperty("schedule_time")
    private Instant scheduleTime;
    
    @JsonProperty("rule")
    private String rule;
    
    @JsonProperty("tracking_data")
    private Map<String, Object> trackingData;
    
    @JsonProperty("business_status")
    private String businessStatus;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("event")
    private List<String> event;
    
    @JsonProperty("created_at")
    private Instant createdAt;
    
    @JsonProperty("updated_at")
    private Instant updatedAt;
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public List<String> getTag() {
        return tag;
    }
    
    public void setTag(List<String> tag) {
        this.tag = tag;
    }
    
    public String getRunner() {
        return runner;
    }
    
    public void setRunner(String runner) {
        this.runner = runner;
    }
    
    public Integer getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
    
    public Instant getScheduleTime() {
        return scheduleTime;
    }
    
    public void setScheduleTime(Instant scheduleTime) {
        this.scheduleTime = scheduleTime;
    }
    
    public String getRule() {
        return rule;
    }
    
    public void setRule(String rule) {
        this.rule = rule;
    }
    
    public Map<String, Object> getTrackingData() {
        return trackingData;
    }
    
    public void setTrackingData(Map<String, Object> trackingData) {
        this.trackingData = trackingData;
    }
    
    public String getBusinessStatus() {
        return businessStatus;
    }
    
    public void setBusinessStatus(String businessStatus) {
        this.businessStatus = businessStatus;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public List<String> getEvent() {
        return event;
    }
    
    public void setEvent(List<String> event) {
        this.event = event;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

