package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Request DTO for SDK event metrics
 */
public class SdkEventMetricsRequest {
    
    @JsonProperty("time_range")
    private TimeRange timeRange;
    
    @JsonProperty("filters")
    private SdkEventFilters filters;
    
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
    
    public SdkEventFilters getFilters() {
        return filters;
    }
    
    public void setFilters(SdkEventFilters filters) {
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
     * SDK event filters
     */
    public static class SdkEventFilters {
        @JsonProperty("payment_method")
        private List<String> paymentMethod;
        
        @JsonProperty("platform")
        private List<String> platform;
        
        @JsonProperty("browser_name")
        private List<String> browserName;
        
        @JsonProperty("source")
        private List<String> source;
        
        @JsonProperty("component")
        private List<String> component;
        
        @JsonProperty("payment_experience")
        private List<String> paymentExperience;
        
        // Getters and Setters
        public List<String> getPaymentMethod() {
            return paymentMethod;
        }
        
        public void setPaymentMethod(List<String> paymentMethod) {
            this.paymentMethod = paymentMethod;
        }
        
        public List<String> getPlatform() {
            return platform;
        }
        
        public void setPlatform(List<String> platform) {
            this.platform = platform;
        }
        
        public List<String> getBrowserName() {
            return browserName;
        }
        
        public void setBrowserName(List<String> browserName) {
            this.browserName = browserName;
        }
        
        public List<String> getSource() {
            return source;
        }
        
        public void setSource(List<String> source) {
            this.source = source;
        }
        
        public List<String> getComponent() {
            return component;
        }
        
        public void setComponent(List<String> component) {
            this.component = component;
        }
        
        public List<String> getPaymentExperience() {
            return paymentExperience;
        }
        
        public void setPaymentExperience(List<String> paymentExperience) {
            this.paymentExperience = paymentExperience;
        }
    }
}

