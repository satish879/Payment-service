package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response DTO for refund metrics
 */
public class RefundMetricsResponse {
    
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
        @JsonProperty("refund_success_rate")
        private Double refundSuccessRate;
        
        @JsonProperty("refund_count")
        private Long refundCount;
        
        @JsonProperty("refund_success_count")
        private Long refundSuccessCount;
        
        @JsonProperty("refund_processed_amount")
        private Long refundProcessedAmount;
        
        @JsonProperty("refund_processed_amount_in_usd")
        private Long refundProcessedAmountInUsd;
        
        @JsonProperty("sessionized_refund_success_rate")
        private Double sessionizedRefundSuccessRate;
        
        @JsonProperty("sessionized_refund_count")
        private Long sessionizedRefundCount;
        
        @JsonProperty("sessionized_refund_success_count")
        private Long sessionizedRefundSuccessCount;
        
        @JsonProperty("sessionized_refund_processed_amount")
        private Long sessionizedRefundProcessedAmount;
        
        @JsonProperty("currency")
        private String currency;
        
        @JsonProperty("refund_status")
        private String refundStatus;
        
        @JsonProperty("connector")
        private String connector;
        
        @JsonProperty("refund_type")
        private String refundType;
        
        @JsonProperty("profile_id")
        private String profileId;
        
        @JsonProperty("refund_reason")
        private String refundReason;
        
        @JsonProperty("refund_error_message")
        private String refundErrorMessage;
        
        @JsonProperty("time_bucket")
        private String timeBucket;
        
        @JsonProperty("start_time")
        private String startTime;
        
        // Getters and Setters
        public Double getRefundSuccessRate() {
            return refundSuccessRate;
        }
        
        public void setRefundSuccessRate(Double refundSuccessRate) {
            this.refundSuccessRate = refundSuccessRate;
        }
        
        public Long getRefundCount() {
            return refundCount;
        }
        
        public void setRefundCount(Long refundCount) {
            this.refundCount = refundCount;
        }
        
        public Long getRefundSuccessCount() {
            return refundSuccessCount;
        }
        
        public void setRefundSuccessCount(Long refundSuccessCount) {
            this.refundSuccessCount = refundSuccessCount;
        }
        
        public Long getRefundProcessedAmount() {
            return refundProcessedAmount;
        }
        
        public void setRefundProcessedAmount(Long refundProcessedAmount) {
            this.refundProcessedAmount = refundProcessedAmount;
        }
        
        public Long getRefundProcessedAmountInUsd() {
            return refundProcessedAmountInUsd;
        }
        
        public void setRefundProcessedAmountInUsd(Long refundProcessedAmountInUsd) {
            this.refundProcessedAmountInUsd = refundProcessedAmountInUsd;
        }
        
        public Double getSessionizedRefundSuccessRate() {
            return sessionizedRefundSuccessRate;
        }
        
        public void setSessionizedRefundSuccessRate(Double sessionizedRefundSuccessRate) {
            this.sessionizedRefundSuccessRate = sessionizedRefundSuccessRate;
        }
        
        public Long getSessionizedRefundCount() {
            return sessionizedRefundCount;
        }
        
        public void setSessionizedRefundCount(Long sessionizedRefundCount) {
            this.sessionizedRefundCount = sessionizedRefundCount;
        }
        
        public Long getSessionizedRefundSuccessCount() {
            return sessionizedRefundSuccessCount;
        }
        
        public void setSessionizedRefundSuccessCount(Long sessionizedRefundSuccessCount) {
            this.sessionizedRefundSuccessCount = sessionizedRefundSuccessCount;
        }
        
        public Long getSessionizedRefundProcessedAmount() {
            return sessionizedRefundProcessedAmount;
        }
        
        public void setSessionizedRefundProcessedAmount(Long sessionizedRefundProcessedAmount) {
            this.sessionizedRefundProcessedAmount = sessionizedRefundProcessedAmount;
        }
        
        public String getCurrency() {
            return currency;
        }
        
        public void setCurrency(String currency) {
            this.currency = currency;
        }
        
        public String getRefundStatus() {
            return refundStatus;
        }
        
        public void setRefundStatus(String refundStatus) {
            this.refundStatus = refundStatus;
        }
        
        public String getConnector() {
            return connector;
        }
        
        public void setConnector(String connector) {
            this.connector = connector;
        }
        
        public String getRefundType() {
            return refundType;
        }
        
        public void setRefundType(String refundType) {
            this.refundType = refundType;
        }
        
        public String getProfileId() {
            return profileId;
        }
        
        public void setProfileId(String profileId) {
            this.profileId = profileId;
        }
        
        public String getRefundReason() {
            return refundReason;
        }
        
        public void setRefundReason(String refundReason) {
            this.refundReason = refundReason;
        }
        
        public String getRefundErrorMessage() {
            return refundErrorMessage;
        }
        
        public void setRefundErrorMessage(String refundErrorMessage) {
            this.refundErrorMessage = refundErrorMessage;
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

