package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for profile acquirer operations
 */
public class ProfileAcquirerResponse {
    
    @JsonProperty("profile_acquirer_id")
    private String profileAcquirerId;
    
    @JsonProperty("profile_id")
    private String profileId;
    
    @JsonProperty("acquirer_id")
    private String acquirerId;
    
    @JsonProperty("acquirer_name")
    private String acquirerName;
    
    @JsonProperty("config")
    private Map<String, Object> config;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("created_at")
    private Instant createdAt;
    
    @JsonProperty("updated_at")
    private Instant updatedAt;
    
    // Getters and Setters
    public String getProfileAcquirerId() {
        return profileAcquirerId;
    }
    
    public void setProfileAcquirerId(String profileAcquirerId) {
        this.profileAcquirerId = profileAcquirerId;
    }
    
    public String getProfileId() {
        return profileId;
    }
    
    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }
    
    public String getAcquirerId() {
        return acquirerId;
    }
    
    public void setAcquirerId(String acquirerId) {
        this.acquirerId = acquirerId;
    }
    
    public String getAcquirerName() {
        return acquirerName;
    }
    
    public void setAcquirerName(String acquirerName) {
        this.acquirerName = acquirerName;
    }
    
    public Map<String, Object> getConfig() {
        return config;
    }
    
    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
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

