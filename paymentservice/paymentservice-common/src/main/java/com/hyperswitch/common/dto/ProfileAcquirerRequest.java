package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Request DTO for profile acquirer operations
 */
public class ProfileAcquirerRequest {
    
    @JsonProperty("acquirer_id")
    private String acquirerId;
    
    @JsonProperty("acquirer_name")
    private String acquirerName;
    
    @JsonProperty("config")
    private Map<String, Object> config;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    // Getters and Setters
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
}

