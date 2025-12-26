package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Request DTO for proxy operations
 */
public class ProxyRequest {
    
    @JsonProperty("url")
    private String url;
    
    @JsonProperty("method")
    private String method;
    
    @JsonProperty("headers")
    private Map<String, String> headers;
    
    @JsonProperty("body")
    private Map<String, Object> body;
    
    // Getters and Setters
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
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

