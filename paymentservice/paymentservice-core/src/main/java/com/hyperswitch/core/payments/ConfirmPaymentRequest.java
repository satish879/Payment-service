package com.hyperswitch.core.payments;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hyperswitch.common.enums.PaymentMethod;

import java.util.Map;

/**
 * Request to confirm/process a payment
 */
public class ConfirmPaymentRequest {
    @JsonProperty("paymentMethod")
    private PaymentMethod paymentMethod;
    
    @JsonProperty("paymentMethodId")
    private String paymentMethodId; // For using saved payment methods
    
    @JsonProperty("paymentMethodData")
    private Map<String, Object> paymentMethodData;
    
    @JsonProperty("returnUrl")
    private String returnUrl;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("offSession")
    private Boolean offSession; // For MIT payments
    
    @JsonProperty("recurringDetails")
    private com.hyperswitch.common.dto.RecurringDetails recurringDetails; // For MIT payments

    // Default constructor for Jackson deserialization
    public ConfirmPaymentRequest() {
    }

    // Builder-based constructor (for programmatic creation)
    private ConfirmPaymentRequest(Builder builder) {
        this.paymentMethod = builder.paymentMethod;
        this.paymentMethodId = builder.paymentMethodId;
        this.paymentMethodData = builder.paymentMethodData;
        this.returnUrl = builder.returnUrl;
        this.metadata = builder.metadata;
        this.offSession = builder.offSession;
        this.recurringDetails = builder.recurringDetails;
    }

    public static Builder builder() {
        return new Builder();
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
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

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Boolean getOffSession() {
        return offSession;
    }

    public void setOffSession(Boolean offSession) {
        this.offSession = offSession;
    }

    public com.hyperswitch.common.dto.RecurringDetails getRecurringDetails() {
        return recurringDetails;
    }

    public void setRecurringDetails(com.hyperswitch.common.dto.RecurringDetails recurringDetails) {
        this.recurringDetails = recurringDetails;
    }

    public static class Builder {
        private PaymentMethod paymentMethod;
        private String paymentMethodId;
        private Map<String, Object> paymentMethodData;
        private String returnUrl;
        private Map<String, Object> metadata;
        private Boolean offSession;
        private com.hyperswitch.common.dto.RecurringDetails recurringDetails;

        public Builder paymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public Builder paymentMethodId(String paymentMethodId) {
            this.paymentMethodId = paymentMethodId;
            return this;
        }

        public Builder paymentMethodData(Map<String, Object> paymentMethodData) {
            this.paymentMethodData = paymentMethodData;
            return this;
        }

        public Builder returnUrl(String returnUrl) {
            this.returnUrl = returnUrl;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder offSession(Boolean offSession) {
            this.offSession = offSession;
            return this;
        }

        public Builder recurringDetails(com.hyperswitch.common.dto.RecurringDetails recurringDetails) {
            this.recurringDetails = recurringDetails;
            return this;
        }

        public ConfirmPaymentRequest build() {
            return new ConfirmPaymentRequest(this);
        }
    }
}
