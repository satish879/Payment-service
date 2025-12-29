package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Response DTO for dummy connector refund
 */
public class DummyConnectorRefundResponse {
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("created")
    private Instant created;
    
    @JsonProperty("payment_amount")
    private Long paymentAmount;
    
    @JsonProperty("refund_amount")
    private Long refundAmount;
    
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
    
    public Long getPaymentAmount() {
        return paymentAmount;
    }
    
    public void setPaymentAmount(Long paymentAmount) {
        this.paymentAmount = paymentAmount;
    }
    
    public Long getRefundAmount() {
        return refundAmount;
    }
    
    public void setRefundAmount(Long refundAmount) {
        this.refundAmount = refundAmount;
    }
}

