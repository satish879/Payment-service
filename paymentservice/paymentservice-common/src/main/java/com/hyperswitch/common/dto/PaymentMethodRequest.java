package com.hyperswitch.common.dto;

import com.hyperswitch.common.types.CustomerId;
import com.hyperswitch.common.types.MerchantId;

import java.util.Map;

/**
 * Request DTO for creating a payment method
 */
public class PaymentMethodRequest {
    private MerchantId merchantId;
    private CustomerId customerId;
    private String paymentMethodType;
    private String paymentMethodSubtype;
    private Map<String, Object> paymentMethodData;
    private String lockerId;
    private Map<String, Object> connectorMandateDetails;
    private String networkTransactionId;
    private String clientSecret;

    /**
     * Default constructor for Jackson deserialization
     */
    public PaymentMethodRequest() {
        // Empty constructor for Jackson deserialization
    }

    public MerchantId getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(MerchantId merchantId) {
        this.merchantId = merchantId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public void setCustomerId(CustomerId customerId) {
        this.customerId = customerId;
    }

    public String getPaymentMethodType() {
        return paymentMethodType;
    }

    public void setPaymentMethodType(String paymentMethodType) {
        this.paymentMethodType = paymentMethodType;
    }

    public String getPaymentMethodSubtype() {
        return paymentMethodSubtype;
    }

    public void setPaymentMethodSubtype(String paymentMethodSubtype) {
        this.paymentMethodSubtype = paymentMethodSubtype;
    }

    public Map<String, Object> getPaymentMethodData() {
        return paymentMethodData;
    }

    public void setPaymentMethodData(Map<String, Object> paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

    public String getLockerId() {
        return lockerId;
    }

    public void setLockerId(String lockerId) {
        this.lockerId = lockerId;
    }

    public Map<String, Object> getConnectorMandateDetails() {
        return connectorMandateDetails;
    }

    public void setConnectorMandateDetails(Map<String, Object> connectorMandateDetails) {
        this.connectorMandateDetails = connectorMandateDetails;
    }

    public String getNetworkTransactionId() {
        return networkTransactionId;
    }

    public void setNetworkTransactionId(String networkTransactionId) {
        this.networkTransactionId = networkTransactionId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    // Builder pattern for backward compatibility
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private MerchantId merchantId;
        private CustomerId customerId;
        private String paymentMethodType;
        private String paymentMethodSubtype;
        private Map<String, Object> paymentMethodData;
        private String lockerId;
        private Map<String, Object> connectorMandateDetails;
        private String networkTransactionId;
        private String clientSecret;

        public Builder merchantId(MerchantId merchantId) {
            this.merchantId = merchantId;
            return this;
        }

        public Builder customerId(CustomerId customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder paymentMethodType(String paymentMethodType) {
            this.paymentMethodType = paymentMethodType;
            return this;
        }

        public Builder paymentMethodSubtype(String paymentMethodSubtype) {
            this.paymentMethodSubtype = paymentMethodSubtype;
            return this;
        }

        public Builder paymentMethodData(Map<String, Object> paymentMethodData) {
            this.paymentMethodData = paymentMethodData;
            return this;
        }

        public Builder lockerId(String lockerId) {
            this.lockerId = lockerId;
            return this;
        }

        public Builder connectorMandateDetails(Map<String, Object> connectorMandateDetails) {
            this.connectorMandateDetails = connectorMandateDetails;
            return this;
        }

        public Builder networkTransactionId(String networkTransactionId) {
            this.networkTransactionId = networkTransactionId;
            return this;
        }

        public Builder clientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        public PaymentMethodRequest build() {
            PaymentMethodRequest request = new PaymentMethodRequest();
            request.setMerchantId(this.merchantId);
            request.setCustomerId(this.customerId);
            request.setPaymentMethodType(this.paymentMethodType);
            request.setPaymentMethodSubtype(this.paymentMethodSubtype);
            request.setPaymentMethodData(this.paymentMethodData);
            request.setLockerId(this.lockerId);
            request.setConnectorMandateDetails(this.connectorMandateDetails);
            request.setNetworkTransactionId(this.networkTransactionId);
            request.setClientSecret(this.clientSecret);
            return request;
        }
    }
}
