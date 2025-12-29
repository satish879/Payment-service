package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Request DTO for payment filters
 */
public class PaymentFiltersRequest {
    
    @JsonProperty("time_range")
    private TimeRange timeRange;
    
    @JsonProperty("group_by_names")
    private List<String> groupByNames;
    
    // Getters and Setters
    public TimeRange getTimeRange() {
        return timeRange;
    }
    
    public void setTimeRange(TimeRange timeRange) {
        this.timeRange = timeRange;
    }
    
    public List<String> getGroupByNames() {
        return groupByNames;
    }
    
    public void setGroupByNames(List<String> groupByNames) {
        this.groupByNames = groupByNames;
    }
    
    /**
     * Time range for filters query
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
}

