package com.hyperswitch.core.payments;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hyperswitch.common.types.Amount;

/**
 * Request to capture a payment
 */
public class CapturePaymentRequest {
    @JsonProperty("amount")
    private Amount amount; // For JSON deserialization: {"amount": {"value": 1000, "currencyCode": "USD"}}
    
    @JsonProperty("amountToCapture")
    private Amount amountToCapture; // Alternative field name, null means capture full amount

    // Default constructor for Jackson deserialization
    public CapturePaymentRequest() {
    }

    // Builder-based constructor (for programmatic creation)
    private CapturePaymentRequest(Builder builder) {
        this.amountToCapture = builder.amountToCapture;
        this.amount = builder.amountToCapture; // Map amount to amountToCapture
    }

    public static Builder builder() {
        return new Builder();
    }

    public Amount getAmount() {
        return amount;
    }

    public void setAmount(Amount amount) {
        this.amount = amount;
        // Also set amountToCapture when amount is set
        this.amountToCapture = amount;
    }

    public Amount getAmountToCapture() {
        // Return amountToCapture if set, otherwise return amount
        return amountToCapture != null ? amountToCapture : amount;
    }

    public void setAmountToCapture(Amount amountToCapture) {
        this.amountToCapture = amountToCapture;
        this.amount = amountToCapture;
    }

    public static class Builder {
        private Amount amountToCapture;

        public Builder amountToCapture(Amount amountToCapture) {
            this.amountToCapture = amountToCapture;
            return this;
        }

        public CapturePaymentRequest build() {
            return new CapturePaymentRequest(this);
        }
    }
}
