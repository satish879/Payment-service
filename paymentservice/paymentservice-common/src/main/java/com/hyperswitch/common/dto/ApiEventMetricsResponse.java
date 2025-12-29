package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response DTO for API event metrics
 */
public class ApiEventMetricsResponse {
    
    @JsonProperty("buckets")
    private List<MetricsBucket> buckets;
    
    @JsonProperty("total_count")
    private Long totalCount;
    
    // Getters and Setters
    public List<MetricsBucket> getBuckets() {
        return buckets;
    }
    
    public void setBuckets(List<MetricsBucket> buckets) {
        this.buckets = buckets;
    }
    
    public Long getTotalCount() {
        return totalCount;
    }
    
    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }
    
    /**
     * Metrics bucket containing values and dimensions
     */
    public static class MetricsBucket {
        @JsonProperty("latency")
        private Long latency;
        
        @JsonProperty("api_count")
        private Long apiCount;
        
        @JsonProperty("status_code_count")
        private Long statusCodeCount;
        
        @JsonProperty("status_code")
        private Long statusCode;
        
        @JsonProperty("flow_type")
        private String flowType;
        
        @JsonProperty("api_flow")
        private String apiFlow;
        
        @JsonProperty("time_bucket")
        private String timeBucket;
        
        @JsonProperty("start_time")
        private String startTime;
        
        // Getters and Setters
        public Long getLatency() {
            return latency;
        }
        
        public void setLatency(Long latency) {
            this.latency = latency;
        }
        
        public Long getApiCount() {
            return apiCount;
        }
        
        public void setApiCount(Long apiCount) {
            this.apiCount = apiCount;
        }
        
        public Long getStatusCodeCount() {
            return statusCodeCount;
        }
        
        public void setStatusCodeCount(Long statusCodeCount) {
            this.statusCodeCount = statusCodeCount;
        }
        
        public Long getStatusCode() {
            return statusCode;
        }
        
        public void setStatusCode(Long statusCode) {
            this.statusCode = statusCode;
        }
        
        public String getFlowType() {
            return flowType;
        }
        
        public void setFlowType(String flowType) {
            this.flowType = flowType;
        }
        
        public String getApiFlow() {
            return apiFlow;
        }
        
        public void setApiFlow(String apiFlow) {
            this.apiFlow = apiFlow;
        }
        
        public String getTimeBucket() {
            return timeBucket;
        }
        
        public void setTimeBucket(String timeBucket) {
            this.timeBucket = timeBucket;
        }
        
        public String getStartTime() {
            return startTime;
        }
        
        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }
    }
}

