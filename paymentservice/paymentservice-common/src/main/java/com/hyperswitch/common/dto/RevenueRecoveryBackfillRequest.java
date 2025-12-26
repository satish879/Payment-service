package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for revenue recovery data backfill
 */
public class RevenueRecoveryBackfillRequest {
    
    @JsonProperty("bin_number")
    private String binNumber;
    
    @JsonProperty("customer_id_resp")
    private String customerIdResp;
    
    @JsonProperty("connector_payment_id")
    private String connectorPaymentId;
    
    @JsonProperty("token")
    private String token;
    
    @JsonProperty("exp_date")
    private String expDate;
    
    @JsonProperty("card_network")
    private String cardNetwork;
    
    @JsonProperty("payment_method_sub_type")
    private String paymentMethodSubType;
    
    @JsonProperty("clean_bank_name")
    private String cleanBankName;
    
    @JsonProperty("country_name")
    private String countryName;
    
    @JsonProperty("daily_retry_history")
    private String dailyRetryHistory;
    
    @JsonProperty("is_active")
    private Boolean isActive;
    
    @JsonProperty("account_update_history")
    private List<Map<String, Object>> accountUpdateHistory;
    
    // Getters and Setters
    public String getBinNumber() {
        return binNumber;
    }
    
    public void setBinNumber(String binNumber) {
        this.binNumber = binNumber;
    }
    
    public String getCustomerIdResp() {
        return customerIdResp;
    }
    
    public void setCustomerIdResp(String customerIdResp) {
        this.customerIdResp = customerIdResp;
    }
    
    public String getConnectorPaymentId() {
        return connectorPaymentId;
    }
    
    public void setConnectorPaymentId(String connectorPaymentId) {
        this.connectorPaymentId = connectorPaymentId;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getExpDate() {
        return expDate;
    }
    
    public void setExpDate(String expDate) {
        this.expDate = expDate;
    }
    
    public String getCardNetwork() {
        return cardNetwork;
    }
    
    public void setCardNetwork(String cardNetwork) {
        this.cardNetwork = cardNetwork;
    }
    
    public String getPaymentMethodSubType() {
        return paymentMethodSubType;
    }
    
    public void setPaymentMethodSubType(String paymentMethodSubType) {
        this.paymentMethodSubType = paymentMethodSubType;
    }
    
    public String getCleanBankName() {
        return cleanBankName;
    }
    
    public void setCleanBankName(String cleanBankName) {
        this.cleanBankName = cleanBankName;
    }
    
    public String getCountryName() {
        return countryName;
    }
    
    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }
    
    public String getDailyRetryHistory() {
        return dailyRetryHistory;
    }
    
    public void setDailyRetryHistory(String dailyRetryHistory) {
        this.dailyRetryHistory = dailyRetryHistory;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public List<Map<String, Object>> getAccountUpdateHistory() {
        return accountUpdateHistory;
    }
    
    public void setAccountUpdateHistory(List<Map<String, Object>> accountUpdateHistory) {
        this.accountUpdateHistory = accountUpdateHistory;
    }
}

