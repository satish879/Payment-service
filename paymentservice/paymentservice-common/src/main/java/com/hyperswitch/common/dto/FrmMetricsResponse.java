package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response DTO for FRM (fraud) metrics
 */
public class FrmMetricsResponse {
    
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
        @JsonProperty("frm_triggered_attempts")
        private Long frmTriggeredAttempts;
        
        @JsonProperty("frm_blocked_rate")
        private Double frmBlockedRate;
        
        @JsonProperty("frm_status")
        private String frmStatus;
        
        @JsonProperty("frm_name")
        private String frmName;
        
        @JsonProperty("frm_transaction_type")
        private String frmTransactionType;
        
        @JsonProperty("time_bucket")
        private String timeBucket;
        
        @JsonProperty("start_time")
        private String startTime;
        
        // Getters and Setters
        public Long getFrmTriggeredAttempts() {
            return frmTriggeredAttempts;
        }
        
        public void setFrmTriggeredAttempts(Long frmTriggeredAttempts) {
            this.frmTriggeredAttempts = frmTriggeredAttempts;
        }
        
        public Double getFrmBlockedRate() {
            return frmBlockedRate;
        }
        
        public void setFrmBlockedRate(Double frmBlockedRate) {
            this.frmBlockedRate = frmBlockedRate;
        }
        
        public String getFrmStatus() {
            return frmStatus;
        }
        
        public void setFrmStatus(String frmStatus) {
            this.frmStatus = frmStatus;
        }
        
        public String getFrmName() {
            return frmName;
        }
        
        public void setFrmName(String frmName) {
            this.frmName = frmName;
        }
        
        public String getFrmTransactionType() {
            return frmTransactionType;
        }
        
        public void setFrmTransactionType(String frmTransactionType) {
            this.frmTransactionType = frmTransactionType;
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

