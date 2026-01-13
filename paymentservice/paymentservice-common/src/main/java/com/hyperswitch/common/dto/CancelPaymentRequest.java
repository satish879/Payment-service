package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Request DTO for cancelling a payment
 */
public class CancelPaymentRequest {
    @JsonProperty("cancellation_reason")
    private String cancellationReason;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    /**
     * Default constructor for Jackson deserialization
     */
    public CancelPaymentRequest() {
        // Empty constructor for Jackson deserialization
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    @JsonProperty("cancellation_reason")
    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @JsonProperty("metadata")
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    // Builder pattern for backward compatibility
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String cancellationReason;
        private Map<String, Object> metadata;

        public Builder cancellationReason(String cancellationReason) {
            this.cancellationReason = cancellationReason;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public CancelPaymentRequest build() {
            CancelPaymentRequest request = new CancelPaymentRequest();
            request.setCancellationReason(this.cancellationReason);
            request.setMetadata(this.metadata);
            return request;
        }
    }
}
