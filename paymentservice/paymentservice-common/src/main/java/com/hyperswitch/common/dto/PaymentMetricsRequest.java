package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for payment metrics
 */
public class PaymentMetricsRequest {
    
    @JsonProperty("time_range")
    private TimeRange timeRange;
    
    @JsonProperty("filters")
    private PaymentFilters filters;
    
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
    
    public PaymentFilters getFilters() {
        return filters;
    }
    
    public void setFilters(PaymentFilters filters) {
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
     * Payment filters
     */
    public static class PaymentFilters {
        @JsonProperty("currency")
        private List<String> currency;
        
        @JsonProperty("status")
        private List<String> status;
        
        @JsonProperty("connector")
        private List<String> connector;
        
        @JsonProperty("auth_type")
        private List<String> authType;
        
        @JsonProperty("payment_method")
        private List<String> paymentMethod;
        
        @JsonProperty("payment_method_type")
        private List<String> paymentMethodType;
        
        @JsonProperty("profile_id")
        private List<String> profileId;
        
        @JsonProperty("merchant_id")
        private List<String> merchantId;
        
        @JsonProperty("card_network")
        private List<String> cardNetwork;
        
        @JsonProperty("error_reason")
        private List<String> errorReason;
        
        @JsonProperty("routing_approach")
        private List<String> routingApproach;
        
        // Getters and Setters
        public List<String> getCurrency() {
            return currency;
        }
        
        public void setCurrency(List<String> currency) {
            this.currency = currency;
        }
        
        public List<String> getStatus() {
            return status;
        }
        
        public void setStatus(List<String> status) {
            this.status = status;
        }
        
        public List<String> getConnector() {
            return connector;
        }
        
        public void setConnector(List<String> connector) {
            this.connector = connector;
        }
        
        public List<String> getAuthType() {
            return authType;
        }
        
        public void setAuthType(List<String> authType) {
            this.authType = authType;
        }
        
        public List<String> getPaymentMethod() {
            return paymentMethod;
        }
        
        public void setPaymentMethod(List<String> paymentMethod) {
            this.paymentMethod = paymentMethod;
        }
        
        public List<String> getPaymentMethodType() {
            return paymentMethodType;
        }
        
        public void setPaymentMethodType(List<String> paymentMethodType) {
            this.paymentMethodType = paymentMethodType;
        }
        
        public List<String> getProfileId() {
            return profileId;
        }
        
        public void setProfileId(List<String> profileId) {
            this.profileId = profileId;
        }
        
        public List<String> getMerchantId() {
            return merchantId;
        }
        
        public void setMerchantId(List<String> merchantId) {
            this.merchantId = merchantId;
        }
        
        public List<String> getCardNetwork() {
            return cardNetwork;
        }
        
        public void setCardNetwork(List<String> cardNetwork) {
            this.cardNetwork = cardNetwork;
        }
        
        public List<String> getErrorReason() {
            return errorReason;
        }
        
        public void setErrorReason(List<String> errorReason) {
            this.errorReason = errorReason;
        }
        
        public List<String> getRoutingApproach() {
            return routingApproach;
        }
        
        public void setRoutingApproach(List<String> routingApproach) {
            this.routingApproach = routingApproach;
        }
    }
}

