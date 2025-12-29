package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Request DTO for dispute metrics
 */
public class DisputeMetricsRequest {
    
    @JsonProperty("time_range")
    private TimeRange timeRange;
    
    @JsonProperty("filters")
    private DisputeFilters filters;
    
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
    
    public DisputeFilters getFilters() {
        return filters;
    }
    
    public void setFilters(DisputeFilters filters) {
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
     * Dispute filters
     */
    public static class DisputeFilters {
        @JsonProperty("dispute_stage")
        private List<String> disputeStage;
        
        @JsonProperty("connector")
        private List<String> connector;
        
        @JsonProperty("currency")
        private List<String> currency;
        
        // Getters and Setters
        public List<String> getDisputeStage() {
            return disputeStage;
        }
        
        public void setDisputeStage(List<String> disputeStage) {
            this.disputeStage = disputeStage;
        }
        
        public List<String> getConnector() {
            return connector;
        }
        
        public void setConnector(List<String> connector) {
            this.connector = connector;
        }
        
        public List<String> getCurrency() {
            return currency;
        }
        
        public void setCurrency(List<String> currency) {
            this.currency = currency;
        }
    }
}

