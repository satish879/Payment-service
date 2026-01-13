package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hyperswitch.common.types.Amount;
import java.util.Map;

/**
 * Request DTO for updating a payment
 */
public class UpdatePaymentRequest {
    @JsonProperty("amount")
    private Amount amount;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("return_url")
    private String returnUrl;

    /**
     * Default constructor for Jackson deserialization
     */
    public UpdatePaymentRequest() {
        // Empty constructor for Jackson deserialization
    }

    public Amount getAmount() {
        return amount;
    }

    @JsonProperty("amount")
    public void setAmount(Amount amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @JsonProperty("metadata")
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    @JsonProperty("return_url")
    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    // Builder pattern for backward compatibility
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Amount amount;
        private String description;
        private Map<String, Object> metadata;
        private String returnUrl;

        public Builder amount(Amount amount) {
            this.amount = amount;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder returnUrl(String returnUrl) {
            this.returnUrl = returnUrl;
            return this;
        }

        public UpdatePaymentRequest build() {
            UpdatePaymentRequest request = new UpdatePaymentRequest();
            request.setAmount(this.amount);
            request.setDescription(this.description);
            request.setMetadata(this.metadata);
            request.setReturnUrl(this.returnUrl);
            return request;
        }
    }
}
