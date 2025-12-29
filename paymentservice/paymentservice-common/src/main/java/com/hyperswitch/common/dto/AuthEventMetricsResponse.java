package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response DTO for auth event metrics
 */
public class AuthEventMetricsResponse {
    
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
        @JsonProperty("authentication_count")
        private Long authenticationCount;
        
        @JsonProperty("authentication_attempt_count")
        private Long authenticationAttemptCount;
        
        @JsonProperty("authentication_success_count")
        private Long authenticationSuccessCount;
        
        @JsonProperty("challenge_flow_count")
        private Long challengeFlowCount;
        
        @JsonProperty("challenge_attempt_count")
        private Long challengeAttemptCount;
        
        @JsonProperty("challenge_success_count")
        private Long challengeSuccessCount;
        
        @JsonProperty("frictionless_flow_count")
        private Long frictionlessFlowCount;
        
        @JsonProperty("frictionless_success_count")
        private Long frictionlessSuccessCount;
        
        @JsonProperty("error_message_count")
        private Long errorMessageCount;
        
        @JsonProperty("authentication_funnel")
        private Long authenticationFunnel;
        
        @JsonProperty("authentication_exemption_approved_count")
        private Long authenticationExemptionApprovedCount;
        
        @JsonProperty("authentication_exemption_requested_count")
        private Long authenticationExemptionRequestedCount;
        
        @JsonProperty("authentication_status")
        private String authenticationStatus;
        
        @JsonProperty("trans_status")
        private String transStatus;
        
        @JsonProperty("authentication_type")
        private String authenticationType;
        
        @JsonProperty("error_message")
        private String errorMessage;
        
        @JsonProperty("authentication_connector")
        private String authenticationConnector;
        
        @JsonProperty("message_version")
        private String messageVersion;
        
        @JsonProperty("platform")
        private String platform;
        
        @JsonProperty("currency")
        private String currency;
        
        @JsonProperty("merchant_country")
        private String merchantCountry;
        
        @JsonProperty("billing_country")
        private String billingCountry;
        
        @JsonProperty("shipping_country")
        private String shippingCountry;
        
        @JsonProperty("issuer_country")
        private String issuerCountry;
        
        @JsonProperty("scheme_name")
        private String schemeName;
        
        @JsonProperty("exemption_requested")
        private Boolean exemptionRequested;
        
        @JsonProperty("exemption_accepted")
        private Boolean exemptionAccepted;
        
        @JsonProperty("time_bucket")
        private String timeBucket;
        
        @JsonProperty("start_time")
        private String startTime;
        
        // Getters and Setters
        public Long getAuthenticationCount() {
            return authenticationCount;
        }
        
        public void setAuthenticationCount(Long authenticationCount) {
            this.authenticationCount = authenticationCount;
        }
        
        public Long getAuthenticationAttemptCount() {
            return authenticationAttemptCount;
        }
        
        public void setAuthenticationAttemptCount(Long authenticationAttemptCount) {
            this.authenticationAttemptCount = authenticationAttemptCount;
        }
        
        public Long getAuthenticationSuccessCount() {
            return authenticationSuccessCount;
        }
        
        public void setAuthenticationSuccessCount(Long authenticationSuccessCount) {
            this.authenticationSuccessCount = authenticationSuccessCount;
        }
        
        public Long getChallengeFlowCount() {
            return challengeFlowCount;
        }
        
        public void setChallengeFlowCount(Long challengeFlowCount) {
            this.challengeFlowCount = challengeFlowCount;
        }
        
        public Long getChallengeAttemptCount() {
            return challengeAttemptCount;
        }
        
        public void setChallengeAttemptCount(Long challengeAttemptCount) {
            this.challengeAttemptCount = challengeAttemptCount;
        }
        
        public Long getChallengeSuccessCount() {
            return challengeSuccessCount;
        }
        
        public void setChallengeSuccessCount(Long challengeSuccessCount) {
            this.challengeSuccessCount = challengeSuccessCount;
        }
        
        public Long getFrictionlessFlowCount() {
            return frictionlessFlowCount;
        }
        
        public void setFrictionlessFlowCount(Long frictionlessFlowCount) {
            this.frictionlessFlowCount = frictionlessFlowCount;
        }
        
        public Long getFrictionlessSuccessCount() {
            return frictionlessSuccessCount;
        }
        
        public void setFrictionlessSuccessCount(Long frictionlessSuccessCount) {
            this.frictionlessSuccessCount = frictionlessSuccessCount;
        }
        
        public Long getErrorMessageCount() {
            return errorMessageCount;
        }
        
        public void setErrorMessageCount(Long errorMessageCount) {
            this.errorMessageCount = errorMessageCount;
        }
        
        public Long getAuthenticationFunnel() {
            return authenticationFunnel;
        }
        
        public void setAuthenticationFunnel(Long authenticationFunnel) {
            this.authenticationFunnel = authenticationFunnel;
        }
        
        public Long getAuthenticationExemptionApprovedCount() {
            return authenticationExemptionApprovedCount;
        }
        
        public void setAuthenticationExemptionApprovedCount(Long authenticationExemptionApprovedCount) {
            this.authenticationExemptionApprovedCount = authenticationExemptionApprovedCount;
        }
        
        public Long getAuthenticationExemptionRequestedCount() {
            return authenticationExemptionRequestedCount;
        }
        
        public void setAuthenticationExemptionRequestedCount(Long authenticationExemptionRequestedCount) {
            this.authenticationExemptionRequestedCount = authenticationExemptionRequestedCount;
        }
        
        public String getAuthenticationStatus() {
            return authenticationStatus;
        }
        
        public void setAuthenticationStatus(String authenticationStatus) {
            this.authenticationStatus = authenticationStatus;
        }
        
        public String getTransStatus() {
            return transStatus;
        }
        
        public void setTransStatus(String transStatus) {
            this.transStatus = transStatus;
        }
        
        public String getAuthenticationType() {
            return authenticationType;
        }
        
        public void setAuthenticationType(String authenticationType) {
            this.authenticationType = authenticationType;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
        
        public String getAuthenticationConnector() {
            return authenticationConnector;
        }
        
        public void setAuthenticationConnector(String authenticationConnector) {
            this.authenticationConnector = authenticationConnector;
        }
        
        public String getMessageVersion() {
            return messageVersion;
        }
        
        public void setMessageVersion(String messageVersion) {
            this.messageVersion = messageVersion;
        }
        
        public String getPlatform() {
            return platform;
        }
        
        public void setPlatform(String platform) {
            this.platform = platform;
        }
        
        public String getCurrency() {
            return currency;
        }
        
        public void setCurrency(String currency) {
            this.currency = currency;
        }
        
        public String getMerchantCountry() {
            return merchantCountry;
        }
        
        public void setMerchantCountry(String merchantCountry) {
            this.merchantCountry = merchantCountry;
        }
        
        public String getBillingCountry() {
            return billingCountry;
        }
        
        public void setBillingCountry(String billingCountry) {
            this.billingCountry = billingCountry;
        }
        
        public String getShippingCountry() {
            return shippingCountry;
        }
        
        public void setShippingCountry(String shippingCountry) {
            this.shippingCountry = shippingCountry;
        }
        
        public String getIssuerCountry() {
            return issuerCountry;
        }
        
        public void setIssuerCountry(String issuerCountry) {
            this.issuerCountry = issuerCountry;
        }
        
        public String getSchemeName() {
            return schemeName;
        }
        
        public void setSchemeName(String schemeName) {
            this.schemeName = schemeName;
        }
        
        public Boolean getExemptionRequested() {
            return exemptionRequested;
        }
        
        public void setExemptionRequested(Boolean exemptionRequested) {
            this.exemptionRequested = exemptionRequested;
        }
        
        public Boolean getExemptionAccepted() {
            return exemptionAccepted;
        }
        
        public void setExemptionAccepted(Boolean exemptionAccepted) {
            this.exemptionAccepted = exemptionAccepted;
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

