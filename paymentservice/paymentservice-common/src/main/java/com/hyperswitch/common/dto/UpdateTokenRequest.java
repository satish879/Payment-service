package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for updating token in recovery data backfill
 */
public class UpdateTokenRequest {
    
    @JsonProperty("connector_customer_id")
    private String connectorCustomerId;
    
    @JsonProperty("token")
    private String token;
    
    @JsonProperty("exp_date")
    private String expDate;
    
    // Getters and Setters
    public String getConnectorCustomerId() {
        return connectorCustomerId;
    }
    
    public void setConnectorCustomerId(String connectorCustomerId) {
        this.connectorCustomerId = connectorCustomerId;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getExpDate() {
        return expDate;
    }
    
    public void setExpDate(String expDate) {
        this.expDate = expDate;
    }
}

