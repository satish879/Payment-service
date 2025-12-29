package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for dummy connector payment
 */
public class DummyConnectorPaymentResponse {
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("amount")
    private Long amount;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("created")
    private Instant created;
    
    @JsonProperty("payment_method_type")
    private String paymentMethodType;
    
    @JsonProperty("next_action")
    private Map<String, Object> nextAction;
    
    // Getters and Setters
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
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
    
    public Instant getCreated() {
        return created;
    }
    
    public void setCreated(Instant created) {
        this.created = created;
    }
    
    public String getPaymentMethodType() {
        return paymentMethodType;
    }
    
    public void setPaymentMethodType(String paymentMethodType) {
        this.paymentMethodType = paymentMethodType;
    }
    
    public Map<String, Object> getNextAction() {
        return nextAction;
    }
    
    public void setNextAction(Map<String, Object> nextAction) {
        this.nextAction = nextAction;
    }
}

