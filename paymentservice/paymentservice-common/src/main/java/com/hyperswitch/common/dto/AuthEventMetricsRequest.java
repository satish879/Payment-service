package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Request DTO for auth event metrics
 */
public class AuthEventMetricsRequest {
    
    @JsonProperty("time_range")
    private TimeRange timeRange;
    
    @JsonProperty("filters")
    private AuthEventFilters filters;
    
    @JsonProperty("metrics")
    private List<String> metrics;
    
    @JsonProperty("dimensions")
    private List<String> dimensions;
    
    @JsonProperty("group_by")
    private List<String> groupBy;
    
    // Getters and Setters
    public TimeRange getTimeRange() {
        return timeRange;
    }
    
    public void setTimeRange(TimeRange timeRange) {
        this.timeRange = timeRange;
    }
    
    public AuthEventFilters getFilters() {
        return filters;
    }
    
    public void setFilters(AuthEventFilters filters) {
        this.filters = filters;
    }
    
    public List<String> getMetrics() {
        return metrics;
    }
    
    public void setMetrics(List<String> metrics) {
        this.metrics = metrics;
    }
    
    public List<String> getDimensions() {
        return dimensions;
    }
    
    public void setDimensions(List<String> dimensions) {
        this.dimensions = dimensions;
    }
    
    public List<String> getGroupBy() {
        return groupBy;
    }
    
    public void setGroupBy(List<String> groupBy) {
        this.groupBy = groupBy;
    }
    
    /**
     * Time range for metrics query
     */
    public static class TimeRange {
        @JsonProperty("start_time")
        private String startTime;
        
        @JsonProperty("end_time")
        private String endTime;
        
        public String getStartTime() {
            return startTime;
        }
        
        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }
        
        public String getEndTime() {
            return endTime;
        }
        
        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }
    }
    
    /**
     * Auth event filters
     */
    public static class AuthEventFilters {
        @JsonProperty("authentication_status")
        private List<String> authenticationStatus;
        
        @JsonProperty("trans_status")
        private List<String> transStatus;
        
        @JsonProperty("authentication_type")
        private List<String> authenticationType;
        
        @JsonProperty("error_message")
        private List<String> errorMessage;
        
        @JsonProperty("authentication_connector")
        private List<String> authenticationConnector;
        
        @JsonProperty("message_version")
        private List<String> messageVersion;
        
        @JsonProperty("platform")
        private List<String> platform;
        
        @JsonProperty("currency")
        private List<String> currency;
        
        @JsonProperty("merchant_country")
        private List<String> merchantCountry;
        
        @JsonProperty("billing_country")
        private List<String> billingCountry;
        
        @JsonProperty("shipping_country")
        private List<String> shippingCountry;
        
        @JsonProperty("issuer_country")
        private List<String> issuerCountry;
        
        @JsonProperty("scheme_name")
        private List<String> schemeName;
        
        @JsonProperty("exemption_requested")
        private List<Boolean> exemptionRequested;
        
        @JsonProperty("exemption_accepted")
        private List<Boolean> exemptionAccepted;
        
        // Getters and Setters
        public List<String> getAuthenticationStatus() {
            return authenticationStatus;
        }
        
        public void setAuthenticationStatus(List<String> authenticationStatus) {
            this.authenticationStatus = authenticationStatus;
        }
        
        public List<String> getTransStatus() {
            return transStatus;
        }
        
        public void setTransStatus(List<String> transStatus) {
            this.transStatus = transStatus;
        }
        
        public List<String> getAuthenticationType() {
            return authenticationType;
        }
        
        public void setAuthenticationType(List<String> authenticationType) {
            this.authenticationType = authenticationType;
        }
        
        public List<String> getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(List<String> errorMessage) {
            this.errorMessage = errorMessage;
        }
        
        public List<String> getAuthenticationConnector() {
            return authenticationConnector;
        }
        
        public void setAuthenticationConnector(List<String> authenticationConnector) {
            this.authenticationConnector = authenticationConnector;
        }
        
        public List<String> getMessageVersion() {
            return messageVersion;
        }
        
        public void setMessageVersion(List<String> messageVersion) {
            this.messageVersion = messageVersion;
        }
        
        public List<String> getPlatform() {
            return platform;
        }
        
        public void setPlatform(List<String> platform) {
            this.platform = platform;
        }
        
        public List<String> getCurrency() {
            return currency;
        }
        
        public void setCurrency(List<String> currency) {
            this.currency = currency;
        }
        
        public List<String> getMerchantCountry() {
            return merchantCountry;
        }
        
        public void setMerchantCountry(List<String> merchantCountry) {
            this.merchantCountry = merchantCountry;
        }
        
        public List<String> getBillingCountry() {
            return billingCountry;
        }
        
        public void setBillingCountry(List<String> billingCountry) {
            this.billingCountry = billingCountry;
        }
        
        public List<String> getShippingCountry() {
            return shippingCountry;
        }
        
        public void setShippingCountry(List<String> shippingCountry) {
            this.shippingCountry = shippingCountry;
        }
        
        public List<String> getIssuerCountry() {
            return issuerCountry;
        }
        
        public void setIssuerCountry(List<String> issuerCountry) {
            this.issuerCountry = issuerCountry;
        }
        
        public List<String> getSchemeName() {
            return schemeName;
        }
        
        public void setSchemeName(List<String> schemeName) {
            this.schemeName = schemeName;
        }
        
        public List<Boolean> getExemptionRequested() {
            return exemptionRequested;
        }
        
        public void setExemptionRequested(List<Boolean> exemptionRequested) {
            this.exemptionRequested = exemptionRequested;
        }
        
        public List<Boolean> getExemptionAccepted() {
            return exemptionAccepted;
        }
        
        public void setExemptionAccepted(List<Boolean> exemptionAccepted) {
            this.exemptionAccepted = exemptionAccepted;
        }
    }
}

