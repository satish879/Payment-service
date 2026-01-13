package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hyperswitch.common.types.MerchantId;

import java.util.Map;

/**
 * Request DTO for creating or updating a customer
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerRequest {
    private MerchantId merchantId;
    private String name;
    private String email;
    private String phone;
    private String phoneCountryCode;
    private String description;
    private Map<String, Object> metadata;
    private String addressId;
    private String defaultPaymentMethodId;

    /**
     * Default constructor for Jackson deserialization
     */
    public CustomerRequest() {
        // Empty constructor for Jackson deserialization
    }

    @JsonProperty("merchantId")
    public MerchantId getMerchantId() {
        return merchantId;
    }

    @JsonProperty("merchantId")
    public void setMerchantId(MerchantId merchantId) {
        this.merchantId = merchantId;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("email")
    public String getEmail() {
        return email;
    }

    @JsonProperty("email")
    public void setEmail(String email) {
        this.email = email;
    }

    @JsonProperty("phone")
    public String getPhone() {
        return phone;
    }

    @JsonProperty("phone")
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhoneCountryCode() {
        return phoneCountryCode;
    }

    public void setPhoneCountryCode(String phoneCountryCode) {
        this.phoneCountryCode = phoneCountryCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("metadata")
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @JsonProperty("metadata")
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getAddressId() {
        return addressId;
    }

    public void setAddressId(String addressId) {
        this.addressId = addressId;
    }

    public String getDefaultPaymentMethodId() {
        return defaultPaymentMethodId;
    }

    public void setDefaultPaymentMethodId(String defaultPaymentMethodId) {
        this.defaultPaymentMethodId = defaultPaymentMethodId;
    }

    // Builder pattern for backward compatibility
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private MerchantId merchantId;
        private String name;
        private String email;
        private String phone;
        private String phoneCountryCode;
        private String description;
        private Map<String, Object> metadata;
        private String addressId;
        private String defaultPaymentMethodId;

        public Builder merchantId(MerchantId merchantId) {
            this.merchantId = merchantId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder phoneCountryCode(String phoneCountryCode) {
            this.phoneCountryCode = phoneCountryCode;
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

        public Builder addressId(String addressId) {
            this.addressId = addressId;
            return this;
        }

        public Builder defaultPaymentMethodId(String defaultPaymentMethodId) {
            this.defaultPaymentMethodId = defaultPaymentMethodId;
            return this;
        }

        public CustomerRequest build() {
            CustomerRequest request = new CustomerRequest();
            request.setMerchantId(this.merchantId);
            request.setName(this.name);
            request.setEmail(this.email);
            request.setPhone(this.phone);
            request.setPhoneCountryCode(this.phoneCountryCode);
            request.setDescription(this.description);
            request.setMetadata(this.metadata);
            request.setAddressId(this.addressId);
            request.setDefaultPaymentMethodId(this.defaultPaymentMethodId);
            return request;
        }
    }
}
