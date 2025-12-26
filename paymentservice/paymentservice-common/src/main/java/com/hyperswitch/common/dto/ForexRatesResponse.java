package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Response DTO for forex rates
 */
public class ForexRatesResponse {
    
    @JsonProperty("base_currency")
    private String baseCurrency;
    
    @JsonProperty("rates")
    private Map<String, Double> rates;
    
    @JsonProperty("timestamp")
    private Long timestamp;
    
    // Getters and Setters
    public String getBaseCurrency() {
        return baseCurrency;
    }
    
    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }
    
    public Map<String, Double> getRates() {
        return rates;
    }
    
    public void setRates(Map<String, Double> rates) {
        this.rates = rates;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}

