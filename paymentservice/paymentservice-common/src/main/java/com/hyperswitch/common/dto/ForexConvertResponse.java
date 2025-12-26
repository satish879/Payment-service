package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for forex conversion
 */
public class ForexConvertResponse {
    
    @JsonProperty("amount")
    private Long amount;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("converted_amount")
    private Long convertedAmount;
    
    @JsonProperty("converted_currency")
    private String convertedCurrency;
    
    @JsonProperty("exchange_rate")
    private Double exchangeRate;
    
    // Getters and Setters
    public Long getAmount() {
        return amount;
    }
    
    public void setAmount(Long amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public Long getConvertedAmount() {
        return convertedAmount;
    }
    
    public void setConvertedAmount(Long convertedAmount) {
        this.convertedAmount = convertedAmount;
    }
    
    public String getConvertedCurrency() {
        return convertedCurrency;
    }
    
    public void setConvertedCurrency(String convertedCurrency) {
        this.convertedCurrency = convertedCurrency;
    }
    
    public Double getExchangeRate() {
        return exchangeRate;
    }
    
    public void setExchangeRate(Double exchangeRate) {
        this.exchangeRate = exchangeRate;
    }
}

