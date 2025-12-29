package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Request DTO for API event metrics
 */
public class ApiEventMetricsRequest {
    
    @JsonProperty("time_range")
    private TimeRange timeRange;
    
    @JsonProperty("filters")
    private ApiEventFilters filters;
    
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
    
    public ApiEventFilters getFilters() {
        return filters;
    }
    
    public void setFilters(ApiEventFilters filters) {
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
     * API event filters
     */
    public static class ApiEventFilters {
        @JsonProperty("status_code")
        private List<Long> statusCode;
        
        @JsonProperty("flow_type")
        private List<String> flowType;
        
        @JsonProperty("api_flow")
        private List<String> apiFlow;
        
        // Getters and Setters
        public List<Long> getStatusCode() {
            return statusCode;
        }
        
        public void setStatusCode(List<Long> statusCode) {
            this.statusCode = statusCode;
        }
        
        public List<String> getFlowType() {
            return flowType;
        }
        
        public void setFlowType(List<String> flowType) {
            this.flowType = flowType;
        }
        
        public List<String> getApiFlow() {
            return apiFlow;
        }
        
        public void setApiFlow(List<String> apiFlow) {
            this.apiFlow = apiFlow;
        }
    }
}

