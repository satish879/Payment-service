package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Request DTO for refund metrics
 */
public class RefundMetricsRequest {
    
    @JsonProperty("time_range")
    private TimeRange timeRange;
    
    @JsonProperty("filters")
    private RefundFilters filters;
    
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
    
    public RefundFilters getFilters() {
        return filters;
    }
    
    public void setFilters(RefundFilters filters) {
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
     * Refund filters
     */
    public static class RefundFilters {
        @JsonProperty("currency")
        private List<String> currency;
        
        @JsonProperty("refund_status")
        private List<String> refundStatus;
        
        @JsonProperty("connector")
        private List<String> connector;
        
        @JsonProperty("refund_type")
        private List<String> refundType;
        
        @JsonProperty("profile_id")
        private List<String> profileId;
        
        @JsonProperty("refund_reason")
        private List<String> refundReason;
        
        @JsonProperty("refund_error_message")
        private List<String> refundErrorMessage;
        
        // Getters and Setters
        public List<String> getCurrency() {
            return currency;
        }
        
        public void setCurrency(List<String> currency) {
            this.currency = currency;
        }
        
        public List<String> getRefundStatus() {
            return refundStatus;
        }
        
        public void setRefundStatus(List<String> refundStatus) {
            this.refundStatus = refundStatus;
        }
        
        public List<String> getConnector() {
            return connector;
        }
        
        public void setConnector(List<String> connector) {
            this.connector = connector;
        }
        
        public List<String> getRefundType() {
            return refundType;
        }
        
        public void setRefundType(List<String> refundType) {
            this.refundType = refundType;
        }
        
        public List<String> getProfileId() {
            return profileId;
        }
        
        public void setProfileId(List<String> profileId) {
            this.profileId = profileId;
        }
        
        public List<String> getRefundReason() {
            return refundReason;
        }
        
        public void setRefundReason(List<String> refundReason) {
            this.refundReason = refundReason;
        }
        
        public List<String> getRefundErrorMessage() {
            return refundErrorMessage;
        }
        
        public void setRefundErrorMessage(List<String> refundErrorMessage) {
            this.refundErrorMessage = refundErrorMessage;
        }
    }
}

