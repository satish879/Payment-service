package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Request DTO for Apple Pay certificates migration
 */
public class ApplePayCertificatesMigrationRequest {
    
    @JsonProperty("merchant_ids")
    private List<String> merchantIds;
    
    // Getters and Setters
    public List<String> getMerchantIds() {
        return merchantIds;
    }
    
    public void setMerchantIds(List<String> merchantIds) {
        this.merchantIds = merchantIds;
    }
}

