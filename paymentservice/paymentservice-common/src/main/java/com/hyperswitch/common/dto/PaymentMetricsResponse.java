package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for payment metrics
 */
public class PaymentMetricsResponse {
    
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
        @JsonProperty("payment_success_rate")
        private Double paymentSuccessRate;
        
        @JsonProperty("payment_count")
        private Long paymentCount;
        
        @JsonProperty("payment_success_count")
        private Long paymentSuccessCount;
        
        @JsonProperty("payment_processed_amount")
        private Long paymentProcessedAmount;
        
        @JsonProperty("payment_processed_amount_in_usd")
        private Long paymentProcessedAmountInUsd;
        
        @JsonProperty("avg_ticket_size")
        private Double avgTicketSize;
        
        @JsonProperty("retries_count")
        private Long retriesCount;
        
        @JsonProperty("connector_success_rate")
        private Double connectorSuccessRate;
        
        @JsonProperty("debit_routed_transaction_count")
        private Long debitRoutedTransactionCount;
        
        @JsonProperty("debit_routing_savings")
        private Long debitRoutingSavings;
        
        @JsonProperty("debit_routing_savings_in_usd")
        private Long debitRoutingSavingsInUsd;
        
        @JsonProperty("currency")
        private String currency;
        
        @JsonProperty("status")
        private String status;
        
        @JsonProperty("connector")
        private String connector;
        
        @JsonProperty("authentication_type")
        private String authenticationType;
        
        @JsonProperty("payment_method")
        private String paymentMethod;
        
        @JsonProperty("payment_method_type")
        private String paymentMethodType;
        
        @JsonProperty("profile_id")
        private String profileId;
        
        @JsonProperty("merchant_id")
        private String merchantId;
        
        @JsonProperty("card_network")
        private String cardNetwork;
        
        @JsonProperty("time_bucket")
        private String timeBucket;
        
        @JsonProperty("start_time")
        private String startTime;
        
        // Getters and Setters
        public Double getPaymentSuccessRate() {
            return paymentSuccessRate;
        }
        
        public void setPaymentSuccessRate(Double paymentSuccessRate) {
            this.paymentSuccessRate = paymentSuccessRate;
        }
        
        public Long getPaymentCount() {
            return paymentCount;
        }
        
        public void setPaymentCount(Long paymentCount) {
            this.paymentCount = paymentCount;
        }
        
        public Long getPaymentSuccessCount() {
            return paymentSuccessCount;
        }
        
        public void setPaymentSuccessCount(Long paymentSuccessCount) {
            this.paymentSuccessCount = paymentSuccessCount;
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
        
        public Double getAvgTicketSize() {
            return avgTicketSize;
        }
        
        public void setAvgTicketSize(Double avgTicketSize) {
            this.avgTicketSize = avgTicketSize;
        }
        
        public Long getRetriesCount() {
            return retriesCount;
        }
        
        public void setRetriesCount(Long retriesCount) {
            this.retriesCount = retriesCount;
        }
        
        public Double getConnectorSuccessRate() {
            return connectorSuccessRate;
        }
        
        public void setConnectorSuccessRate(Double connectorSuccessRate) {
            this.connectorSuccessRate = connectorSuccessRate;
        }
        
        public Long getDebitRoutedTransactionCount() {
            return debitRoutedTransactionCount;
        }
        
        public void setDebitRoutedTransactionCount(Long debitRoutedTransactionCount) {
            this.debitRoutedTransactionCount = debitRoutedTransactionCount;
        }
        
        public Long getDebitRoutingSavings() {
            return debitRoutingSavings;
        }
        
        public void setDebitRoutingSavings(Long debitRoutingSavings) {
            this.debitRoutingSavings = debitRoutingSavings;
        }
        
        public Long getDebitRoutingSavingsInUsd() {
            return debitRoutingSavingsInUsd;
        }
        
        public void setDebitRoutingSavingsInUsd(Long debitRoutingSavingsInUsd) {
            this.debitRoutingSavingsInUsd = debitRoutingSavingsInUsd;
        }
        
        public String getCurrency() {
            return currency;
        }
        
        public void setCurrency(String currency) {
            this.currency = currency;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
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

