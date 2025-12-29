package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response DTO for routing metrics
 */
public class RoutingMetricsResponse {
    
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
        @JsonProperty("routing_count")
        private Long routingCount;
        
        @JsonProperty("routing_success_count")
        private Long routingSuccessCount;
        
        @JsonProperty("routing_success_rate")
        private Double routingSuccessRate;
        
        @JsonProperty("routing_decision_count")
        private Long routingDecisionCount;
        
        @JsonProperty("connector_selection_count")
        private Long connectorSelectionCount;
        
        @JsonProperty("algorithm_performance_score")
        private Double algorithmPerformanceScore;
        
        @JsonProperty("routing_latency_avg")
        private Double routingLatencyAvg;
        
        @JsonProperty("routing_latency_p95")
        private Double routingLatencyP95;
        
        @JsonProperty("routing_latency_p99")
        private Double routingLatencyP99;
        
        @JsonProperty("fallback_count")
        private Long fallbackCount;
        
        @JsonProperty("fallback_rate")
        private Double fallbackRate;
        
        @JsonProperty("currency")
        private String currency;
        
        @JsonProperty("connector")
        private String connector;
        
        @JsonProperty("routing_algorithm")
        private String routingAlgorithm;
        
        @JsonProperty("profile_id")
        private String profileId;
        
        @JsonProperty("merchant_id")
        private String merchantId;
        
        @JsonProperty("payment_method")
        private String paymentMethod;
        
        @JsonProperty("payment_method_type")
        private String paymentMethodType;
        
        @JsonProperty("card_network")
        private String cardNetwork;
        
        @JsonProperty("time_bucket")
        private String timeBucket;
        
        @JsonProperty("start_time")
        private String startTime;
        
        // Getters and Setters
        public Long getRoutingCount() {
            return routingCount;
        }
        
        public void setRoutingCount(Long routingCount) {
            this.routingCount = routingCount;
        }
        
        public Long getRoutingSuccessCount() {
            return routingSuccessCount;
        }
        
        public void setRoutingSuccessCount(Long routingSuccessCount) {
            this.routingSuccessCount = routingSuccessCount;
        }
        
        public Double getRoutingSuccessRate() {
            return routingSuccessRate;
        }
        
        public void setRoutingSuccessRate(Double routingSuccessRate) {
            this.routingSuccessRate = routingSuccessRate;
        }
        
        public Long getRoutingDecisionCount() {
            return routingDecisionCount;
        }
        
        public void setRoutingDecisionCount(Long routingDecisionCount) {
            this.routingDecisionCount = routingDecisionCount;
        }
        
        public Long getConnectorSelectionCount() {
            return connectorSelectionCount;
        }
        
        public void setConnectorSelectionCount(Long connectorSelectionCount) {
            this.connectorSelectionCount = connectorSelectionCount;
        }
        
        public Double getAlgorithmPerformanceScore() {
            return algorithmPerformanceScore;
        }
        
        public void setAlgorithmPerformanceScore(Double algorithmPerformanceScore) {
            this.algorithmPerformanceScore = algorithmPerformanceScore;
        }
        
        public Double getRoutingLatencyAvg() {
            return routingLatencyAvg;
        }
        
        public void setRoutingLatencyAvg(Double routingLatencyAvg) {
            this.routingLatencyAvg = routingLatencyAvg;
        }
        
        public Double getRoutingLatencyP95() {
            return routingLatencyP95;
        }
        
        public void setRoutingLatencyP95(Double routingLatencyP95) {
            this.routingLatencyP95 = routingLatencyP95;
        }
        
        public Double getRoutingLatencyP99() {
            return routingLatencyP99;
        }
        
        public void setRoutingLatencyP99(Double routingLatencyP99) {
            this.routingLatencyP99 = routingLatencyP99;
        }
        
        public Long getFallbackCount() {
            return fallbackCount;
        }
        
        public void setFallbackCount(Long fallbackCount) {
            this.fallbackCount = fallbackCount;
        }
        
        public Double getFallbackRate() {
            return fallbackRate;
        }
        
        public void setFallbackRate(Double fallbackRate) {
            this.fallbackRate = fallbackRate;
        }
        
        public String getCurrency() {
            return currency;
        }
        
        public void setCurrency(String currency) {
            this.currency = currency;
        }
        
        public String getConnector() {
            return connector;
        }
        
        public void setConnector(String connector) {
            this.connector = connector;
        }
        
        public String getRoutingAlgorithm() {
            return routingAlgorithm;
        }
        
        public void setRoutingAlgorithm(String routingAlgorithm) {
            this.routingAlgorithm = routingAlgorithm;
        }
        
        public String getProfileId() {
            return profileId;
        }
        
        public void setProfileId(String profileId) {
            this.profileId = profileId;
        }
        
        public String getMerchantId() {
            return merchantId;
        }
        
        public void setMerchantId(String merchantId) {
            this.merchantId = merchantId;
        }
        
        public String getPaymentMethod() {
            return paymentMethod;
        }
        
        public void setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
        }
        
        public String getPaymentMethodType() {
            return paymentMethodType;
        }
        
        public void setPaymentMethodType(String paymentMethodType) {
            this.paymentMethodType = paymentMethodType;
        }
        
        public String getCardNetwork() {
            return cardNetwork;
        }
        
        public void setCardNetwork(String cardNetwork) {
            this.cardNetwork = cardNetwork;
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

