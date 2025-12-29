package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response DTO for payment intent metrics
 */
public class PaymentIntentMetricsResponse {
    
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
        @JsonProperty("successful_smart_retries")
        private Long successfulSmartRetries;
        
        @JsonProperty("total_smart_retries")
        private Long totalSmartRetries;
        
        @JsonProperty("smart_retried_amount")
        private Long smartRetriedAmount;
        
        @JsonProperty("smart_retried_amount_in_usd")
        private Long smartRetriedAmountInUsd;
        
        @JsonProperty("payment_intent_count")
        private Long paymentIntentCount;
        
        @JsonProperty("successful_payments")
        private Long successfulPayments;
        
        @JsonProperty("total_payments")
        private Long totalPayments;
        
        @JsonProperty("payments_success_rate")
        private Double paymentsSuccessRate;
        
        @JsonProperty("payment_processed_amount")
        private Long paymentProcessedAmount;
        
        @JsonProperty("payment_processed_amount_in_usd")
        private Long paymentProcessedAmountInUsd;
        
        @JsonProperty("payment_processed_count")
        private Long paymentProcessedCount;
        
        @JsonProperty("sessionized_successful_smart_retries")
        private Long sessionizedSuccessfulSmartRetries;
        
        @JsonProperty("sessionized_total_smart_retries")
        private Long sessionizedTotalSmartRetries;
        
        @JsonProperty("sessionized_smart_retried_amount")
        private Long sessionizedSmartRetriedAmount;
        
        @JsonProperty("sessionized_payment_intent_count")
        private Long sessionizedPaymentIntentCount;
        
        @JsonProperty("sessionized_payments_success_rate")
        private Double sessionizedPaymentsSuccessRate;
        
        @JsonProperty("sessionized_payment_processed_amount")
        private Long sessionizedPaymentProcessedAmount;
        
        @JsonProperty("status")
        private String status;
        
        @JsonProperty("currency")
        private String currency;
        
        @JsonProperty("profile_id")
        private String profileId;
        
        @JsonProperty("connector")
        private String connector;
        
        @JsonProperty("authentication_type")
        private String authenticationType;
        
        @JsonProperty("payment_method")
        private String paymentMethod;
        
        @JsonProperty("payment_method_type")
        private String paymentMethodType;
        
        @JsonProperty("card_network")
        private String cardNetwork;
        
        @JsonProperty("merchant_id")
        private String merchantId;
        
        @JsonProperty("card_last_4")
        private String cardLast4;
        
        @JsonProperty("card_issuer")
        private String cardIssuer;
        
        @JsonProperty("error_reason")
        private String errorReason;
        
        @JsonProperty("time_bucket")
        private String timeBucket;
        
        @JsonProperty("start_time")
        private String startTime;
        
        // Getters and Setters
        public Long getSuccessfulSmartRetries() {
            return successfulSmartRetries;
        }
        
        public void setSuccessfulSmartRetries(Long successfulSmartRetries) {
            this.successfulSmartRetries = successfulSmartRetries;
        }
        
        public Long getTotalSmartRetries() {
            return totalSmartRetries;
        }
        
        public void setTotalSmartRetries(Long totalSmartRetries) {
            this.totalSmartRetries = totalSmartRetries;
        }
        
        public Long getSmartRetriedAmount() {
            return smartRetriedAmount;
        }
        
        public void setSmartRetriedAmount(Long smartRetriedAmount) {
            this.smartRetriedAmount = smartRetriedAmount;
        }
        
        public Long getSmartRetriedAmountInUsd() {
            return smartRetriedAmountInUsd;
        }
        
        public void setSmartRetriedAmountInUsd(Long smartRetriedAmountInUsd) {
            this.smartRetriedAmountInUsd = smartRetriedAmountInUsd;
        }
        
        public Long getPaymentIntentCount() {
            return paymentIntentCount;
        }
        
        public void setPaymentIntentCount(Long paymentIntentCount) {
            this.paymentIntentCount = paymentIntentCount;
        }
        
        public Long getSuccessfulPayments() {
            return successfulPayments;
        }
        
        public void setSuccessfulPayments(Long successfulPayments) {
            this.successfulPayments = successfulPayments;
        }
        
        public Long getTotalPayments() {
            return totalPayments;
        }
        
        public void setTotalPayments(Long totalPayments) {
            this.totalPayments = totalPayments;
        }
        
        public Double getPaymentsSuccessRate() {
            return paymentsSuccessRate;
        }
        
        public void setPaymentsSuccessRate(Double paymentsSuccessRate) {
            this.paymentsSuccessRate = paymentsSuccessRate;
        }
        
        public Long getPaymentProcessedAmount() {
            return paymentProcessedAmount;
        }
        
        public void setPaymentProcessedAmount(Long paymentProcessedAmount) {
            this.paymentProcessedAmount = paymentProcessedAmount;
        }
        
        public Long getPaymentProcessedAmountInUsd() {
            return paymentProcessedAmountInUsd;
        }
        
        public void setPaymentProcessedAmountInUsd(Long paymentProcessedAmountInUsd) {
            this.paymentProcessedAmountInUsd = paymentProcessedAmountInUsd;
        }
        
        public Long getPaymentProcessedCount() {
            return paymentProcessedCount;
        }
        
        public void setPaymentProcessedCount(Long paymentProcessedCount) {
            this.paymentProcessedCount = paymentProcessedCount;
        }
        
        public Long getSessionizedSuccessfulSmartRetries() {
            return sessionizedSuccessfulSmartRetries;
        }
        
        public void setSessionizedSuccessfulSmartRetries(Long sessionizedSuccessfulSmartRetries) {
            this.sessionizedSuccessfulSmartRetries = sessionizedSuccessfulSmartRetries;
        }
        
        public Long getSessionizedTotalSmartRetries() {
            return sessionizedTotalSmartRetries;
        }
        
        public void setSessionizedTotalSmartRetries(Long sessionizedTotalSmartRetries) {
            this.sessionizedTotalSmartRetries = sessionizedTotalSmartRetries;
        }
        
        public Long getSessionizedSmartRetriedAmount() {
            return sessionizedSmartRetriedAmount;
        }
        
        public void setSessionizedSmartRetriedAmount(Long sessionizedSmartRetriedAmount) {
            this.sessionizedSmartRetriedAmount = sessionizedSmartRetriedAmount;
        }
        
        public Long getSessionizedPaymentIntentCount() {
            return sessionizedPaymentIntentCount;
        }
        
        public void setSessionizedPaymentIntentCount(Long sessionizedPaymentIntentCount) {
            this.sessionizedPaymentIntentCount = sessionizedPaymentIntentCount;
        }
        
        public Double getSessionizedPaymentsSuccessRate() {
            return sessionizedPaymentsSuccessRate;
        }
        
        public void setSessionizedPaymentsSuccessRate(Double sessionizedPaymentsSuccessRate) {
            this.sessionizedPaymentsSuccessRate = sessionizedPaymentsSuccessRate;
        }
        
        public Long getSessionizedPaymentProcessedAmount() {
            return sessionizedPaymentProcessedAmount;
        }
        
        public void setSessionizedPaymentProcessedAmount(Long sessionizedPaymentProcessedAmount) {
            this.sessionizedPaymentProcessedAmount = sessionizedPaymentProcessedAmount;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public String getCurrency() {
            return currency;
        }
        
        public void setCurrency(String currency) {
            this.currency = currency;
        }
        
        public String getProfileId() {
            return profileId;
        }
        
        public void setProfileId(String profileId) {
            this.profileId = profileId;
        }
        
        public String getConnector() {
            return connector;
        }
        
        public void setConnector(String connector) {
            this.connector = connector;
        }
        
        public String getAuthenticationType() {
            return authenticationType;
        }
        
        public void setAuthenticationType(String authenticationType) {
            this.authenticationType = authenticationType;
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
        
        public String getMerchantId() {
            return merchantId;
        }
        
        public void setMerchantId(String merchantId) {
            this.merchantId = merchantId;
        }
        
        public String getCardLast4() {
            return cardLast4;
        }
        
        public void setCardLast4(String cardLast4) {
            this.cardLast4 = cardLast4;
        }
        
        public String getCardIssuer() {
            return cardIssuer;
        }
        
        public void setCardIssuer(String cardIssuer) {
            this.cardIssuer = cardIssuer;
        }
        
        public String getErrorReason() {
            return errorReason;
        }
        
        public void setErrorReason(String errorReason) {
            this.errorReason = errorReason;
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

