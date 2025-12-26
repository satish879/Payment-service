package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Response DTO for proxy operations
 */
public class ProxyResponse {
    
    @JsonProperty("status_code")
    private Integer statusCode;
    
    @JsonProperty("headers")
    private Map<String, String> headers;
    
    @JsonProperty("body")
    private Map<String, Object> body;
    
    // Getters and Setters
    public Integer getStatusCode() {
        return statusCode;
    }
    
    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    
    public Map<String, Object> getBody() {
        return body;
    }
    
    public void setBody(Map<String, Object> body) {
        this.body = body;
    }
}

