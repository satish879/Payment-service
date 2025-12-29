package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Request DTO for creating dummy connector payment
 */
public class DummyConnectorPaymentRequest {
    
    @JsonProperty("amount")
    private Long amount;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("payment_method_data")
    private Map<String, Object> paymentMethodData;
    
    @JsonProperty("return_url")
    private String returnUrl;
    
    @JsonProperty("connector")
    private String connector;
    
    // Getters and Setters
    public Long getAmount() {
        return amount;
    }
    
    public void setAmount(Long amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public Map<String, Object> getPaymentMethodData() {
        return paymentMethodData;
    }
    
    public void setPaymentMethodData(Map<String, Object> paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }
    
    public String getReturnUrl() {
        return returnUrl;
    }
    
    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }
    
    public String getConnector() {
        return connector;
    }
    
    public void setConnector(String connector) {
        this.connector = connector;
    }
}

