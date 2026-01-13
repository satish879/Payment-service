package com.hyperswitch.core.payments;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hyperswitch.common.types.Amount;

/**
 * Request to process a refund
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundRequest {
    @JsonProperty("amount")
    private Amount amount; // null means full refund
    
    @JsonProperty("reason")
    private String reason;
    
    @JsonProperty("metadata")
    private java.util.Map<String, Object> metadata;

    /**
     * Default constructor for Jackson deserialization
     */
    public RefundRequest() {
        // Empty constructor for Jackson deserialization
    }

    public Amount getAmount() {
        return amount;
    }

    @JsonProperty("amount")
    public void setAmount(Amount amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    @JsonProperty("reason")
    public void setReason(String reason) {
        this.reason = reason;
    }

    public java.util.Map<String, Object> getMetadata() {
        return metadata;
    }

    @JsonProperty("metadata")
    public void setMetadata(java.util.Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    // Builder pattern for backward compatibility
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Amount amount;
        private String reason;
        private java.util.Map<String, Object> metadata;

        public Builder amount(Amount amount) {
            this.amount = amount;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder metadata(java.util.Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public RefundRequest build() {
            RefundRequest request = new RefundRequest();
            request.setAmount(this.amount);
            request.setReason(this.reason);
            request.setMetadata(this.metadata);
            return request;
        }
    }
}
