package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response DTO for SDK event metrics
 */
public class SdkEventMetricsResponse {
    
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
        @JsonProperty("payment_attempts")
        private Long paymentAttempts;
        
        @JsonProperty("payment_methods_call_count")
        private Long paymentMethodsCallCount;
        
        @JsonProperty("average_payment_time")
        private Long averagePaymentTime;
        
        @JsonProperty("load_time")
        private Long loadTime;
        
        @JsonProperty("sdk_rendered_count")
        private Long sdkRenderedCount;
        
        @JsonProperty("sdk_initiated_count")
        private Long sdkInitiatedCount;
        
        @JsonProperty("payment_method_selected_count")
        private Long paymentMethodSelectedCount;
        
        @JsonProperty("payment_data_filled_count")
        private Long paymentDataFilledCount;
        
        @JsonProperty("payment_method")
        private String paymentMethod;
        
        @JsonProperty("platform")
        private String platform;
        
        @JsonProperty("browser_name")
        private String browserName;
        
        @JsonProperty("source")
        private String source;
        
        @JsonProperty("component")
        private String component;
        
        @JsonProperty("payment_experience")
        private String paymentExperience;
        
        @JsonProperty("time_bucket")
        private String timeBucket;
        
        @JsonProperty("start_time")
        private String startTime;
        
        // Getters and Setters
        public Long getPaymentAttempts() {
            return paymentAttempts;
        }
        
        public void setPaymentAttempts(Long paymentAttempts) {
            this.paymentAttempts = paymentAttempts;
        }
        
        public Long getPaymentMethodsCallCount() {
            return paymentMethodsCallCount;
        }
        
        public void setPaymentMethodsCallCount(Long paymentMethodsCallCount) {
            this.paymentMethodsCallCount = paymentMethodsCallCount;
        }
        
        public Long getAveragePaymentTime() {
            return averagePaymentTime;
        }
        
        public void setAveragePaymentTime(Long averagePaymentTime) {
            this.averagePaymentTime = averagePaymentTime;
        }
        
        public Long getLoadTime() {
            return loadTime;
        }
        
        public void setLoadTime(Long loadTime) {
            this.loadTime = loadTime;
        }
        
        public Long getSdkRenderedCount() {
            return sdkRenderedCount;
        }
        
        public void setSdkRenderedCount(Long sdkRenderedCount) {
            this.sdkRenderedCount = sdkRenderedCount;
        }
        
        public Long getSdkInitiatedCount() {
            return sdkInitiatedCount;
        }
        
        public void setSdkInitiatedCount(Long sdkInitiatedCount) {
            this.sdkInitiatedCount = sdkInitiatedCount;
        }
        
        public Long getPaymentMethodSelectedCount() {
            return paymentMethodSelectedCount;
        }
        
        public void setPaymentMethodSelectedCount(Long paymentMethodSelectedCount) {
            this.paymentMethodSelectedCount = paymentMethodSelectedCount;
        }
        
        public Long getPaymentDataFilledCount() {
            return paymentDataFilledCount;
        }
        
        public void setPaymentDataFilledCount(Long paymentDataFilledCount) {
            this.paymentDataFilledCount = paymentDataFilledCount;
        }
        
        public String getPaymentMethod() {
            return paymentMethod;
        }
        
        public void setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
        }
        
        public String getPlatform() {
            return platform;
        }
        
        public void setPlatform(String platform) {
            this.platform = platform;
        }
        
        public String getBrowserName() {
            return browserName;
        }
        
        public void setBrowserName(String browserName) {
            this.browserName = browserName;
        }
        
        public String getSource() {
            return source;
        }
        
        public void setSource(String source) {
            this.source = source;
        }
        
        public String getComponent() {
            return component;
        }
        
        public void setComponent(String component) {
            this.component = component;
        }
        
        public String getPaymentExperience() {
            return paymentExperience;
        }
        
        public void setPaymentExperience(String paymentExperience) {
            this.paymentExperience = paymentExperience;
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

