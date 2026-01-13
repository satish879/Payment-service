package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Request DTO for 3DS authentication
 */
public class ThreeDSRequest {
    @JsonProperty("payment_id")
    private String paymentId;
    
    @JsonProperty("authentication_id")
    private String authenticationId;
    
    @JsonProperty("authentication_data")
    private Map<String, Object> authenticationData;
    
    @JsonProperty("return_url")
    private String returnUrl;

    /**
     * Default constructor for Jackson deserialization
     */
    public ThreeDSRequest() {
        // Empty constructor for Jackson deserialization
    }

    public String getPaymentId() {
        return paymentId;
    }

    @JsonProperty("payment_id")
    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getAuthenticationId() {
        return authenticationId;
    }

    @JsonProperty("authentication_id")
    public void setAuthenticationId(String authenticationId) {
        this.authenticationId = authenticationId;
    }

    public Map<String, Object> getAuthenticationData() {
        return authenticationData;
    }

    @JsonProperty("authentication_data")
    public void setAuthenticationData(Map<String, Object> authenticationData) {
        this.authenticationData = authenticationData;
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
        private String paymentId;
        private String authenticationId;
        private Map<String, Object> authenticationData;
        private String returnUrl;

        public Builder paymentId(String paymentId) {
            this.paymentId = paymentId;
            return this;
        }

        public Builder authenticationId(String authenticationId) {
            this.authenticationId = authenticationId;
            return this;
        }

        public Builder authenticationData(Map<String, Object> authenticationData) {
            this.authenticationData = authenticationData;
            return this;
        }

        public Builder returnUrl(String returnUrl) {
            this.returnUrl = returnUrl;
            return this;
        }

        public ThreeDSRequest build() {
            ThreeDSRequest request = new ThreeDSRequest();
            request.setPaymentId(this.paymentId);
            request.setAuthenticationId(this.authenticationId);
            request.setAuthenticationData(this.authenticationData);
            request.setReturnUrl(this.returnUrl);
            return request;
        }
    }
}
