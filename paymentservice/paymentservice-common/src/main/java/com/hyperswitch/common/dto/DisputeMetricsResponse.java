package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response DTO for dispute metrics
 */
public class DisputeMetricsResponse {
    
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
        @JsonProperty("disputes_challenged")
        private Long disputesChallenged;
        
        @JsonProperty("disputes_won")
        private Long disputesWon;
        
        @JsonProperty("disputes_lost")
        private Long disputesLost;
        
        @JsonProperty("disputed_amount")
        private Long disputedAmount;
        
        @JsonProperty("disputed_amount_in_usd")
        private Long disputedAmountInUsd;
        
        @JsonProperty("dispute_lost_amount")
        private Long disputeLostAmount;
        
        @JsonProperty("dispute_lost_amount_in_usd")
        private Long disputeLostAmountInUsd;
        
        @JsonProperty("total_dispute")
        private Long totalDispute;
        
        @JsonProperty("sessionized_disputes_challenged")
        private Long sessionizedDisputesChallenged;
        
        @JsonProperty("sessionized_disputes_won")
        private Long sessionizedDisputesWon;
        
        @JsonProperty("sessionized_disputes_lost")
        private Long sessionizedDisputesLost;
        
        @JsonProperty("sessionized_disputed_amount")
        private Long sessionizedDisputedAmount;
        
        @JsonProperty("sessionized_dispute_lost_amount")
        private Long sessionizedDisputeLostAmount;
        
        @JsonProperty("dispute_stage")
        private String disputeStage;
        
        @JsonProperty("connector")
        private String connector;
        
        @JsonProperty("currency")
        private String currency;
        
        @JsonProperty("time_bucket")
        private String timeBucket;
        
        @JsonProperty("start_time")
        private String startTime;
        
        // Getters and Setters
        public Long getDisputesChallenged() {
            return disputesChallenged;
        }
        
        public void setDisputesChallenged(Long disputesChallenged) {
            this.disputesChallenged = disputesChallenged;
        }
        
        public Long getDisputesWon() {
            return disputesWon;
        }
        
        public void setDisputesWon(Long disputesWon) {
            this.disputesWon = disputesWon;
        }
        
        public Long getDisputesLost() {
            return disputesLost;
        }
        
        public void setDisputesLost(Long disputesLost) {
            this.disputesLost = disputesLost;
        }
        
        public Long getDisputedAmount() {
            return disputedAmount;
        }
        
        public void setDisputedAmount(Long disputedAmount) {
            this.disputedAmount = disputedAmount;
        }
        
        public Long getDisputedAmountInUsd() {
            return disputedAmountInUsd;
        }
        
        public void setDisputedAmountInUsd(Long disputedAmountInUsd) {
            this.disputedAmountInUsd = disputedAmountInUsd;
        }
        
        public Long getDisputeLostAmount() {
            return disputeLostAmount;
        }
        
        public void setDisputeLostAmount(Long disputeLostAmount) {
            this.disputeLostAmount = disputeLostAmount;
        }
        
        public Long getDisputeLostAmountInUsd() {
            return disputeLostAmountInUsd;
        }
        
        public void setDisputeLostAmountInUsd(Long disputeLostAmountInUsd) {
            this.disputeLostAmountInUsd = disputeLostAmountInUsd;
        }
        
        public Long getTotalDispute() {
            return totalDispute;
        }
        
        public void setTotalDispute(Long totalDispute) {
            this.totalDispute = totalDispute;
        }
        
        public Long getSessionizedDisputesChallenged() {
            return sessionizedDisputesChallenged;
        }
        
        public void setSessionizedDisputesChallenged(Long sessionizedDisputesChallenged) {
            this.sessionizedDisputesChallenged = sessionizedDisputesChallenged;
        }
        
        public Long getSessionizedDisputesWon() {
            return sessionizedDisputesWon;
        }
        
        public void setSessionizedDisputesWon(Long sessionizedDisputesWon) {
            this.sessionizedDisputesWon = sessionizedDisputesWon;
        }
        
        public Long getSessionizedDisputesLost() {
            return sessionizedDisputesLost;
        }
        
        public void setSessionizedDisputesLost(Long sessionizedDisputesLost) {
            this.sessionizedDisputesLost = sessionizedDisputesLost;
        }
        
        public Long getSessionizedDisputedAmount() {
            return sessionizedDisputedAmount;
        }
        
        public void setSessionizedDisputedAmount(Long sessionizedDisputedAmount) {
            this.sessionizedDisputedAmount = sessionizedDisputedAmount;
        }
        
        public Long getSessionizedDisputeLostAmount() {
            return sessionizedDisputeLostAmount;
        }
        
        public void setSessionizedDisputeLostAmount(Long sessionizedDisputeLostAmount) {
            this.sessionizedDisputeLostAmount = sessionizedDisputeLostAmount;
        }
        
        public String getDisputeStage() {
            return disputeStage;
        }
        
        public void setDisputeStage(String disputeStage) {
            this.disputeStage = disputeStage;
        }
        
        public String getConnector() {
            return connector;
        }
        
        public void setConnector(String connector) {
            this.connector = connector;
        }
        
        public String getCurrency() {
            return currency;
        }
        
        public void setCurrency(String currency) {
            this.currency = currency;
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

