package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hyperswitch.common.enums.CaptureMethod;
import com.hyperswitch.common.enums.PaymentMethod;
import com.hyperswitch.common.types.Amount;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request to create a new payment
 */
public class CreatePaymentRequest {
    @NotNull
    @JsonProperty("amount")
    private Amount amount;
    
    @NotNull
    @JsonProperty("merchantId")
    private String merchantId;
    
    @JsonProperty("paymentMethod")
    private PaymentMethod paymentMethod;
    
    @JsonProperty("customerId")
    private String customerId;
    
    @JsonProperty("captureMethod")
    private CaptureMethod captureMethod;
    
    @JsonProperty("confirm")
    private Boolean confirm;
    
    @JsonProperty("returnUrl")
    private String returnUrl;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("authenticationType")
    private String authenticationType;
    
    @JsonProperty("offSession")
    private Boolean offSession; // For MIT payments
    
    @JsonProperty("recurringDetails")
    private RecurringDetails recurringDetails; // For MIT payments
    
    @JsonProperty("paymentType")
    private String paymentType; // "setup_mandate" for zero-dollar authorization

    // Default constructor for Jackson deserialization
    public CreatePaymentRequest() {
    }

    // Builder-based constructor (for programmatic creation)
    private CreatePaymentRequest(Builder builder) {
        this.amount = builder.amount;
        this.merchantId = builder.merchantId;
        this.paymentMethod = builder.paymentMethod;
        this.customerId = builder.customerId;
        this.captureMethod = builder.captureMethod;
        this.confirm = builder.confirm;
        this.returnUrl = builder.returnUrl;
        this.metadata = builder.metadata;
        this.description = builder.description;
        this.authenticationType = builder.authenticationType;
        this.offSession = builder.offSession;
        this.recurringDetails = builder.recurringDetails;
        this.paymentType = builder.paymentType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Amount getAmount() {
        return amount;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getCustomerId() {
        return customerId;
    }

    public CaptureMethod getCaptureMethod() {
        return captureMethod;
    }

    public Boolean getConfirm() {
        return confirm;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthenticationType() {
        return authenticationType;
    }

    public Boolean getOffSession() {
        return offSession;
    }

    public RecurringDetails getRecurringDetails() {
        return recurringDetails;
    }

    public String getPaymentType() {
        return paymentType;
    }

    // Setters for Jackson deserialization
    public void setAmount(Amount amount) {
        this.amount = amount;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public void setCaptureMethod(CaptureMethod captureMethod) {
        this.captureMethod = captureMethod;
    }

    public void setConfirm(Boolean confirm) {
        this.confirm = confirm;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAuthenticationType(String authenticationType) {
        this.authenticationType = authenticationType;
    }

    public void setOffSession(Boolean offSession) {
        this.offSession = offSession;
    }

    public void setRecurringDetails(RecurringDetails recurringDetails) {
        this.recurringDetails = recurringDetails;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public static class Builder {
        private Amount amount;
        private String merchantId;
        private PaymentMethod paymentMethod;
        private String customerId;
        private CaptureMethod captureMethod;
        private Boolean confirm;
        private String returnUrl;
        private Map<String, Object> metadata;
        private String description;
        private String authenticationType;
        private Boolean offSession;
        private RecurringDetails recurringDetails;
        private String paymentType;

        public Builder amount(Amount amount) {
            this.amount = amount;
            return this;
        }

        public Builder merchantId(String merchantId) {
            this.merchantId = merchantId;
            return this;
        }

        public Builder paymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder captureMethod(CaptureMethod captureMethod) {
            this.captureMethod = captureMethod;
            return this;
        }

        public Builder confirm(Boolean confirm) {
            this.confirm = confirm;
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

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder authenticationType(String authenticationType) {
            this.authenticationType = authenticationType;
            return this;
        }

        public Builder offSession(Boolean offSession) {
            this.offSession = offSession;
            return this;
        }

        public Builder recurringDetails(RecurringDetails recurringDetails) {
            this.recurringDetails = recurringDetails;
            return this;
        }

        public Builder paymentType(String paymentType) {
            this.paymentType = paymentType;
            return this;
        }

        public CreatePaymentRequest build() {
            return new CreatePaymentRequest(this);
        }
    }
}

