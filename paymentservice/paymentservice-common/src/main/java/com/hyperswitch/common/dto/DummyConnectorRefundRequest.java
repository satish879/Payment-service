package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for creating dummy connector refund
 */
public class DummyConnectorRefundRequest {
    
    @JsonProperty("amount")
    private Long amount;
    
    @JsonProperty("payment_id")
    private String paymentId;
    
    // Getters and Setters
    public Long getAmount() {
        return amount;
    }
    
    public void setAmount(Long amount) {
        this.amount = amount;
    }
    
    public String getPaymentId() {
        return paymentId;
    }
    
    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }
}

