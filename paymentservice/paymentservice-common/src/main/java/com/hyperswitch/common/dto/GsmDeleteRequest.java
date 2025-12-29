package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for deleting GSM rule
 */
public class GsmDeleteRequest {
    
    @JsonProperty("connector")
    private String connector;
    
    @JsonProperty("flow")
    private String flow;
    
    @JsonProperty("sub_flow")
    private String subFlow;
    
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("message")
    private String message;
    
    // Getters and Setters
    public String getConnector() {
        return connector;
    }
    
    public void setConnector(String connector) {
        this.connector = connector;
    }
    
    public String getFlow() {
        return flow;
    }
    
    public void setFlow(String flow) {
        this.flow = flow;
    }
    
    public String getSubFlow() {
        return subFlow;
    }
    
    public void setSubFlow(String subFlow) {
        this.subFlow = subFlow;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}

