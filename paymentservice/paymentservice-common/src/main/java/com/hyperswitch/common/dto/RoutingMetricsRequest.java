package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Request DTO for routing metrics
 */
public class RoutingMetricsRequest {
    
    @JsonProperty("time_range")
    private TimeRange timeRange;
    
    @JsonProperty("filters")
    private RoutingFilters filters;
    
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
    
    public RoutingFilters getFilters() {
        return filters;
    }
    
    public void setFilters(RoutingFilters filters) {
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
     * Routing filters
     */
    public static class RoutingFilters {
        @JsonProperty("currency")
        private List<String> currency;
        
        @JsonProperty("connector")
        private List<String> connector;
        
        @JsonProperty("routing_algorithm")
        private List<String> routingAlgorithm;
        
        @JsonProperty("profile_id")
        private List<String> profileId;
        
        @JsonProperty("merchant_id")
        private List<String> merchantId;
        
        @JsonProperty("payment_method")
        private List<String> paymentMethod;
        
        @JsonProperty("payment_method_type")
        private List<String> paymentMethodType;
        
        @JsonProperty("card_network")
        private List<String> cardNetwork;
        
        // Getters and Setters
        public List<String> getCurrency() {
            return currency;
        }
        
        public void setCurrency(List<String> currency) {
            this.currency = currency;
        }
        
        public List<String> getConnector() {
            return connector;
        }
        
        public void setConnector(List<String> connector) {
            this.connector = connector;
        }
        
        public List<String> getRoutingAlgorithm() {
            return routingAlgorithm;
        }
        
        public void setRoutingAlgorithm(List<String> routingAlgorithm) {
            this.routingAlgorithm = routingAlgorithm;
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
        
        public List<String> getCardNetwork() {
            return cardNetwork;
        }
        
        public void setCardNetwork(List<String> cardNetwork) {
            this.cardNetwork = cardNetwork;
        }
    }
}

