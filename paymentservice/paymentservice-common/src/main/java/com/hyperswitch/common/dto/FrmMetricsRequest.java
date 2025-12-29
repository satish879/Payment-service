package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Request DTO for FRM (fraud) metrics
 */
public class FrmMetricsRequest {
    
    @JsonProperty("time_range")
    private TimeRange timeRange;
    
    @JsonProperty("filters")
    private FrmFilters filters;
    
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
    
    public FrmFilters getFilters() {
        return filters;
    }
    
    public void setFilters(FrmFilters filters) {
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
     * FRM filters
     */
    public static class FrmFilters {
        @JsonProperty("frm_status")
        private List<String> frmStatus;
        
        @JsonProperty("frm_name")
        private List<String> frmName;
        
        @JsonProperty("frm_transaction_type")
        private List<String> frmTransactionType;
        
        // Getters and Setters
        public List<String> getFrmStatus() {
            return frmStatus;
        }
        
        public void setFrmStatus(List<String> frmStatus) {
            this.frmStatus = frmStatus;
        }
        
        public List<String> getFrmName() {
            return frmName;
        }
        
        public void setFrmName(List<String> frmName) {
            this.frmName = frmName;
        }
        
        public List<String> getFrmTransactionType() {
            return frmTransactionType;
        }
        
        public void setFrmTransactionType(List<String> frmTransactionType) {
            this.frmTransactionType = frmTransactionType;
        }
    }
}

