package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Request DTO for relay operations
 */
public class RelayRequest {
    
    @JsonProperty("connector_resource_id")
    private String connectorResourceId;
    
    @JsonProperty("connector_id")
    private String connectorId;
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("data")
    private Map<String, Object> data;
    
    // Getters and Setters
    public String getConnectorResourceId() {
        return connectorResourceId;
    }
    
    public void setConnectorResourceId(String connectorResourceId) {
        this.connectorResourceId = connectorResourceId;
    }
    
    public String getConnectorId() {
        return connectorId;
    }
    
    public void setConnectorId(String connectorId) {
        this.connectorId = connectorId;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}

